# End-to-end smoke test for core payment flow (dev-local)
# Prereqs: Run these services locally in separate terminals:
#   mvn -pl services/customer-profile spring-boot:run
#   mvn -pl services/account-management spring-boot:run
#   mvn -pl services/beneficiary-management spring-boot:run
#   mvn -pl services/ledger spring-boot:run
#   mvn -pl services/fraud-detection spring-boot:run
#   mvn -pl services/payment-gateway spring-boot:run
# This script:
#   1) Creates a customer (dev onboarding-approved)
#   2) Creates two SAVINGS accounts
#   3) Seeds balance in the source account
#   4) Adds beneficiary and verifies OTP
#   5) Initiates internal transfer via payment-gateway
#   6) Verifies final balances

param(
  [string]$FirstName = "Alice",
  [string]$LastName = "Lee",
  [string]$Email = "alice.lee@example.com",
  [string]$Mobile = "9990001111",
  [decimal]$SeedAmount = 5000,
  [decimal]$TransferAmount = 1000,
  [string]$Currency = "USD",
  [string]$CustBase = "http://localhost:8102",
  [string]$AccBase  = "http://localhost:8103",
  [string]$BenBase  = "http://localhost:8104",
  [string]$PayBase  = "http://localhost:8105"
)

$ErrorActionPreference = "Stop"

function Step($msg) { Write-Host ("==> {0}" -f $msg) -ForegroundColor Cyan }
function Info($msg) { Write-Host $msg -ForegroundColor Yellow }
function Ok($msg)   { Write-Host $msg -ForegroundColor Green }
function Fail($msg) { Write-Host $msg -ForegroundColor Red }

try {
  $cid = [guid]::NewGuid().ToString()
  $headers = @{ "X-Correlation-Id" = $cid; "Content-Type" = "application/json" }
  Info ("CorrelationId: {0}" -f $cid)

  Step "Create customer (dev onboarding-approved)"
  $customerBody = @{
    firstName = $FirstName
    lastName = $LastName
    email = $Email
    mobileNumber = $Mobile
    applicationId = "DEV-APP-1"
  } | ConvertTo-Json -Depth 5
  $customerId = Invoke-RestMethod -Method Post -Uri "$CustBase/api/v1/internal/dev/onboarding-approved" -Headers $headers -Body $customerBody
  Info ("customerId={0}" -f $customerId)

  Step "Create two SAVINGS accounts"
  $accBody = @{ customerId = $customerId; accountType = "SAVINGS" } | ConvertTo-Json
  $fromAccount = Invoke-RestMethod -Method Post -Uri "$AccBase/api/v1/accounts" -Headers $headers -Body $accBody
  $toAccount   = Invoke-RestMethod -Method Post -Uri "$AccBase/api/v1/accounts" -Headers $headers -Body $accBody
  Info ("fromAccount={0}; toAccount={1}" -f $fromAccount, $toAccount)

  Step ("Seed fromAccount with {0}" -f $SeedAmount)
  $adjBody = @{ amount = $SeedAmount } | ConvertTo-Json
  $balFromSeed = Invoke-RestMethod -Method Post -Uri "$AccBase/api/v1/internal/dev/accounts/$fromAccount/adjust-balance" -Headers $headers -Body $adjBody
  Info ("balance(fromAccount after seed)={0}" -f $balFromSeed)

  Step "Add beneficiary pointing to toAccount and verify OTP"
  $benBody = @{ nickname = "Savings2"; accountNumber = $toAccount; bankCode = "OMNI" } | ConvertTo-Json
  $benCreate = Invoke-RestMethod -Method Post -Uri "$BenBase/api/v1/customers/$customerId/beneficiaries" -Headers $headers -Body $benBody
  $beneficiaryId = $benCreate.beneficiaryId; $challengeId = $benCreate.challengeId; $otp = $benCreate.otpDevEcho
  Info ("beneficiaryId={0}; challengeId={1}; otp={2}" -f $beneficiaryId, $challengeId, $otp)
  $verifyBody = @{ owningCustomerId = $customerId; beneficiaryId = $beneficiaryId; challengeId = $challengeId; code = $otp } | ConvertTo-Json
  Invoke-RestMethod -Method Post -Uri "$BenBase/api/v1/beneficiaries/verify-otp" -Headers $headers -Body $verifyBody | Out-Null
  Info "beneficiaryVerified=true"

  Step ("Initiate internal transfer {0} {1} via payment-gateway" -f $TransferAmount, $Currency)
  $payBody = @{ customerId = $customerId; fromAccount = $fromAccount; toAccount = $toAccount; amount = $TransferAmount; currency = $Currency } | ConvertTo-Json
  $init = Invoke-RestMethod -Method Post -Uri "$PayBase/api/v1/payments/internal-transfer" -Headers $headers -Body $payBody
  $paymentId = $init.paymentId; $status = $init.status
  Info ("paymentId={0}; initialStatus={1}" -f $paymentId, $status)

  $finalStatus = $status
  if ($status -ne "COMPLETED") {
    Start-Sleep -Seconds 2
    $finalStatus = Invoke-RestMethod -Method Get -Uri "$PayBase/api/v1/payments/$paymentId/status" -Headers $headers
  }
  Info ("finalStatus={0}" -f $finalStatus)

  Step "Read balances after transfer"
  $balFrom = Invoke-RestMethod -Method Get -Uri "$AccBase/api/v1/accounts/$fromAccount/balance" -Headers $headers
  $balTo   = Invoke-RestMethod -Method Get -Uri "$AccBase/api/v1/accounts/$toAccount/balance" -Headers $headers
  Info ("balance(fromAccount)={0}; balance(toAccount)={1}" -f $balFrom, $balTo)

  Ok ("E2E OK | cid={0} | customerId={1} | from={2} | to={3} | paymentId={4} | status={5} | balFrom={6} | balTo={7}" -f $cid, $customerId, $fromAccount, $toAccount, $paymentId, $finalStatus, $balFrom, $balTo)
} catch {
  Fail ("E2E FAILED: {0}" -f $_.Exception.Message)
  if ($_.Exception.Response) {
    try {
      $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
      $respBody = $reader.ReadToEnd()
      Write-Host "HTTP Response: $respBody" -ForegroundColor DarkRed
    } catch {}
  }
  exit 1
}

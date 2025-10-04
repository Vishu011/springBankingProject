# Starts OmniBank core services locally (dev-open) in separate PowerShell windows
# Optional: -Kafka to run card services with Kafka profile (requires local Kafka broker)
# Optional: -WithFrontend to also start Angular customer app on http://localhost:5173
# Usage examples:
#   pwsh -File scripts/start-dev-open.ps1
#   pwsh -File scripts/start-dev-open.ps1 -WithFrontend
#   pwsh -File scripts/start-dev-open.ps1 -Kafka -WithFrontend

param(
  [switch]$Kafka = $false,
  [switch]$WithFrontend = $false
)

$ErrorActionPreference = "Stop"

function Info($msg) { Write-Host ("==> {0}" -f $msg) -ForegroundColor Cyan }

# Resolve project root
$scriptDir = Split-Path -Parent $PSCommandPath
$root = Split-Path -Parent $scriptDir
$pwsh = "$env:ProgramFiles\PowerShell\7\pwsh.exe"
if (-not (Test-Path $pwsh)) {
  $pwsh = "pwsh" # fallback in PATH
}

function StartServiceWindow($title, $modulePath, [string]$profile = "") {
  $cmd = "mvn -q -pl $modulePath spring-boot:run"
  if ($profile -ne "") {
    $cmd = "\$env:SPRING_PROFILES_ACTIVE='$profile'; $cmd"
  }
  $args = @(
    "-NoExit",
    "-Command",
    $cmd
  )
  Start-Process -FilePath $pwsh -ArgumentList $args -WorkingDirectory $root -WindowStyle Minimized
  Info ("Started {0} | {1}" -f $title, $cmd)
}

Info "Starting OmniBank services (dev-open)"
# Core services for Payments and Cards demos
StartServiceWindow "customer-profile (8102)"      "services/customer-profile"
StartServiceWindow "account-management (8103)"    "services/account-management"
StartServiceWindow "beneficiary-management (8104)" "services/beneficiary-management"
StartServiceWindow "payment-gateway (8105)"       "services/payment-gateway"
StartServiceWindow "ledger (8106)"                "services/ledger"

# Lending (loan-management summary + schedule APIs)
StartServiceWindow "loan-management (8122)"       "services/loan-management"

# Cards
if ($Kafka) {
  Info "Kafka mode requested: starting card services with SPRING_PROFILES_ACTIVE=kafka"
  StartServiceWindow "card-issuance (8130,kafka)"  "services/card-issuance"   "kafka"
  StartServiceWindow "card-management (8131,kafka)" "services/card-management" "kafka"
} else {
  StartServiceWindow "card-issuance (8130)"        "services/card-issuance"
  StartServiceWindow "card-management (8131)"      "services/card-management"
}

# Optional: Angular frontend
if ($WithFrontend) {
  $frontendCmd = "cd frontend/customer-angular; if (-not (Test-Path node_modules)) { npm install }; npx ng serve --port 5173 --open=false"
  $args = @(
    "-NoExit",
    "-Command",
    $frontendCmd
  )
  Start-Process -FilePath $pwsh -ArgumentList $args -WorkingDirectory $root -WindowStyle Minimized
  Info "Started Angular customer app (http://localhost:5173)"
}

Info "All launch commands issued. Check the new PowerShell windows for logs."

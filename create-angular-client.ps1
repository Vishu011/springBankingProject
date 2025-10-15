# ================================
# PowerShell Script to Create a Public Angular Client in Keycloak
# ================================

param(
    [string]$KeycloakHost = "http://localhost:8180",
    [string]$Realm = "bank-realm",
    [string]$AdminUser = "",
    [string]$AdminPass = "",
    [string]$ClientId = "bank-frontend",
    [string]$RedirectUri = "http://localhost:4200/*"
)

Write-Host "`n=== Getting Admin Access Token from Keycloak ==="

# 1️⃣ Get Admin Token
$tokenResponse = Invoke-RestMethod -Method Post `
    -Uri "$KeycloakHost/realms/master/protocol/openid-connect/token" `
    -ContentType "application/x-www-form-urlencoded" `
    -Body @{
        username = $AdminUser
        password = $AdminPass
        grant_type = "password"
        client_id = "admin-cli"
    }

$accessToken = $tokenResponse.access_token

if (-not $accessToken) {
    Write-Host "❌ Failed to get admin token. Check credentials or server URL."
    exit 1
}

Write-Host "✅ Token acquired successfully."

# 2️⃣ Create JSON payload for client
$clientConfig = @{
    clientId = $ClientId
    publicClient = $true
    protocol = "openid-connect"
    redirectUris = @($RedirectUri)
    webOrigins = @("*")
    directAccessGrantsEnabled = $true
    standardFlowEnabled = $true
    rootUrl = "http://localhost:4200"
    baseUrl = "/"
    attributes = @{
        "pkce.code.challenge.method" = "S256"
    }
} | ConvertTo-Json -Depth 5

Write-Host "`n=== Creating Client '$ClientId' in Realm '$Realm' ==="

# 3️⃣ Create Client in Realm
$response = Invoke-RestMethod -Method Post `
    -Uri "$KeycloakHost/admin/realms/$Realm/clients" `
    -Headers @{ Authorization = "Bearer $accessToken"; "Content-Type" = "application/json" } `
    -Body $clientConfig `
    -SkipHttpErrorCheck

if ($response -eq $null) {
    Write-Host "✅ Client '$ClientId' created successfully in realm '$Realm'."
} else {
    Write-Host "⚠️ Response: $($response | ConvertTo-Json -Depth 5)"
}

Write-Host "`n=== Done ==="

[CmdletBinding()]
param(
    [string]$BaseUrl = "http://127.0.0.1:8088",
    [string]$Email = "",
    [string]$Password = "",
    [string]$AccessToken = "",
    [switch]$RequireAuthenticated,
    [switch]$SkipMutation
)

$ErrorActionPreference = "Stop"
$BaseUrl = $BaseUrl.TrimEnd('/')

function Assert-True([bool]$Condition, [string]$Message) {
    if (-not $Condition) {
        throw $Message
    }
}

function Invoke-JsonRequest(
    [string]$Method,
    [string]$Path,
    [object]$Body = $null,
    [string]$Token = ""
) {
    $headers = @{ "X-Request-Id" = "smoke-$([guid]::NewGuid().ToString('N'))" }
    if (-not [string]::IsNullOrWhiteSpace($Token)) {
        $headers["Authorization"] = "Bearer $Token"
    }

    $parameters = @{
        Method = $Method
        Uri = "$BaseUrl$Path"
        Headers = $headers
        TimeoutSec = 15
    }
    if ($null -ne $Body) {
        $parameters["ContentType"] = "application/json; charset=utf-8"
        $parameters["Body"] = $Body | ConvertTo-Json -Depth 8 -Compress
    }
    Invoke-RestMethod @parameters
}

Write-Host "[1/4] Checking gateway health"
$health = Invoke-JsonRequest "GET" "/actuator/health"
Assert-True ($health.status -eq "UP") "Gateway health did not return UP."
$gateway = Invoke-JsonRequest "GET" "/gateway/status"
Assert-True ($gateway.status -eq "UP") "Gateway status endpoint is unavailable."
Assert-True ([bool]$gateway.authenticationBoundary) "Gateway authentication boundary is disabled."

Write-Host "[2/4] Checking unauthenticated request rejection"
$unauthorized = $false
try {
    Invoke-JsonRequest "GET" "/api/v1/home" | Out-Null
} catch {
    $statusCode = [int]$_.Exception.Response.StatusCode
    $unauthorized = $statusCode -eq 401
}
Assert-True $unauthorized "Gateway did not return 401 for unauthenticated /api/v1/home."

if ([string]::IsNullOrWhiteSpace($AccessToken) -and
        -not [string]::IsNullOrWhiteSpace($Email) -and
        -not [string]::IsNullOrWhiteSpace($Password)) {
    Write-Host "[3/4] Signing in with the smoke-test account"
    $login = Invoke-JsonRequest "POST" "/api/auth/login" @{
        email = $Email
        password = $Password
    }
    Assert-True ($login.code -eq "OK") "Login endpoint did not return OK."
    $AccessToken = $login.data.accessToken
}

if ([string]::IsNullOrWhiteSpace($AccessToken)) {
    if ($RequireAuthenticated) {
        throw "Authenticated checks require -AccessToken or both -Email and -Password."
    }
    Write-Host "Public checks passed. Provide a test account or access token for full checks." `
        -ForegroundColor Yellow
    return
}

Write-Host "[3/4] Checking authenticated read endpoints"
foreach ($path in @("/api/v1/home", "/api/v1/usage", "/api/v1/sessions?page=0&size=10")) {
    $response = Invoke-JsonRequest "GET" $path $null $AccessToken
    Assert-True ($response.code -eq "OK") "$path did not return OK."
}

if ($SkipMutation) {
    Write-Host "Authenticated read checks passed." -ForegroundColor Green
    return
}

Write-Host "[4/4] Checking session, message, end and cleanup flows"
$sessionId = $null
try {
    $created = Invoke-JsonRequest "POST" "/api/v1/sessions" @{
        title = "Automated smoke test"
    } $AccessToken
    Assert-True ($created.code -eq "OK") "Session creation failed."
    $sessionId = $created.data.id
    Assert-True (-not [string]::IsNullOrWhiteSpace($sessionId)) "Session creation returned no ID."

    $clientMessageId = "smoke-$([guid]::NewGuid().ToString('N'))"
    $message = Invoke-JsonRequest "POST" "/api/v1/sessions/$sessionId/messages" @{
        clientMessageId = $clientMessageId
        speaker = "SELF"
        content = "Tingjian automated smoke-test message"
    } $AccessToken
    Assert-True ($message.code -eq "OK") "Message creation failed."
    Assert-True ($message.data.clientMessageId -eq $clientMessageId) "Message idempotency ID mismatch."

    $page = Invoke-JsonRequest "GET" "/api/v1/sessions/$sessionId/messages?afterSequence=0&size=10" `
        $null $AccessToken
    Assert-True ($page.code -eq "OK") "Message pagination failed."

    $ended = Invoke-JsonRequest "POST" "/api/v1/sessions/$sessionId/end" $null $AccessToken
    Assert-True ($ended.code -eq "OK") "Ending the session failed."
} finally {
    if (-not [string]::IsNullOrWhiteSpace($sessionId)) {
        try {
            Invoke-JsonRequest "DELETE" "/api/v1/history/$sessionId" $null $AccessToken | Out-Null
        } catch {
            Write-Warning "Smoke-test cleanup failed. Delete history session later: $sessionId"
        }
    }
}

Write-Host "Tingjian gateway, authentication, session, message and usage checks passed." `
    -ForegroundColor Green

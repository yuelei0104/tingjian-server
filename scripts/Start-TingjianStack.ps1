[CmdletBinding()]
param(
    [int]$TimeoutSeconds = 180,
    [switch]$SkipBuild
)

$ErrorActionPreference = "Stop"
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path

function Require-ConfigurationValue([string]$Name) {
    $value = [Environment]::GetEnvironmentVariable($Name)
    if (-not [string]::IsNullOrWhiteSpace($value)) {
        return
    }

    $envFile = Join-Path $repoRoot ".env"
    if (Test-Path $envFile) {
        $line = Get-Content $envFile | Where-Object {
            $_ -match "^\s*$([regex]::Escape($Name))\s*=\s*.+$"
        } | Select-Object -First 1
        if ($null -ne $line) {
            return
        }
    }

    throw "Set $Name in the process environment or the repository .env file. See .env.example."
}

function Wait-ForEndpoint([string]$Uri, [int]$Timeout) {
    $deadline = (Get-Date).AddSeconds($Timeout)
    do {
        try {
            $response = Invoke-RestMethod -Method Get -Uri $Uri -TimeoutSec 5
            if ($response.status -eq "UP") {
                return
            }
        } catch {
            Start-Sleep -Seconds 2
        }
    } while ((Get-Date) -lt $deadline)

    throw "Timed out waiting for $Uri. Run: docker compose logs --tail=200"
}

Require-ConfigurationValue "TINGJIAN_DB_PASSWORD"
Require-ConfigurationValue "TINGJIAN_DB_ROOT_PASSWORD"
Require-ConfigurationValue "TINGJIAN_VERIFICATION_PEPPER"
Require-ConfigurationValue "TINGJIAN_VERIFICATION_WEBHOOK_URL"
Require-ConfigurationValue "TINGJIAN_INTERNAL_SERVICE_TOKEN"

Push-Location $repoRoot
try {
    $arguments = @("compose", "up", "-d")
    if (-not $SkipBuild) {
        $arguments += "--build"
    }
    & docker @arguments
    if ($LASTEXITCODE -ne 0) {
        throw "docker compose failed to start the stack."
    }

    Wait-ForEndpoint "http://127.0.0.1:8088/actuator/health" $TimeoutSeconds
    Wait-ForEndpoint "http://127.0.0.1:8088/gateway/status" $TimeoutSeconds

    Write-Host "Tingjian local stack is ready:" -ForegroundColor Green
    Write-Host "  Gateway: http://127.0.0.1:8088"
    Write-Host "  Monolith: http://127.0.0.1:8080"
    Write-Host "Next: .\scripts\Test-TingjianStack.ps1"
} finally {
    Pop-Location
}

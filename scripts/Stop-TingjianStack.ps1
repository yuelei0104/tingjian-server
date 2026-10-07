[CmdletBinding()]
param(
    [switch]$RemoveVolumes
)

$ErrorActionPreference = "Stop"
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path

Push-Location $repoRoot
try {
    $arguments = @("compose", "down")
    if ($RemoveVolumes) {
        $confirmation = Read-Host "This removes MySQL and Redis volumes. Type DELETE to confirm"
        if ($confirmation -ne "DELETE") {
            throw "Volume removal cancelled."
        }
        $arguments += "--volumes"
    }
    & docker @arguments
    if ($LASTEXITCODE -ne 0) {
        throw "docker compose failed to stop the stack."
    }
} finally {
    Pop-Location
}

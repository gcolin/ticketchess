param(
    [switch]$SkipBuild
)

$ErrorActionPreference = "Stop"

$RepoRoot = Resolve-Path (Join-Path $PSScriptRoot "..\..\..\..")
$JarPath = Join-Path $RepoRoot "target\ticket-chess-1.0.0-fat.jar"
$RemoteHost = $env:TICKETCHESS_INTEGRATION_HOST
if (-not $RemoteHost) {
    throw "Set TICKETCHESS_INTEGRATION_HOST (e.g. pi@192.168.x.x) before publishing."
}
$RemoteDir = "/mnt/nvme/ticketchess/test"
$Remote = "${RemoteHost}:${RemoteDir}"
$ResetScript = "${RemoteDir}/reset.sh"

Set-Location $RepoRoot

if (-not $SkipBuild -and -not (Test-Path $JarPath)) {
    Write-Host "Building fat JAR..."
    mvn package -P pack -DskipTests
    if ($LASTEXITCODE -ne 0) {
        throw "Maven build failed with exit code $LASTEXITCODE"
    }
}

if (-not (Test-Path $JarPath)) {
    throw "JAR not found: $JarPath"
}

Write-Host "Publishing $JarPath to $Remote ..."
scp $JarPath $Remote
if ($LASTEXITCODE -ne 0) {
    throw "SCP failed with exit code $LASTEXITCODE"
}

Write-Host "Running reset script on $RemoteHost ..."
ssh $RemoteHost $ResetScript
if ($LASTEXITCODE -ne 0) {
    throw "reset.sh failed with exit code $LASTEXITCODE"
}

Write-Host "Published and restarted successfully."

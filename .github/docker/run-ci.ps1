param(
    [ValidateSet("test", "package", "all")]
    [string]$Stage = "test"
)

$ErrorActionPreference = "Stop"
$Root = Resolve-Path (Join-Path $PSScriptRoot "../..")
$ComposeFile = Join-Path $PSScriptRoot "docker-compose.yml"

function Get-DockerExe {
    $cmd = Get-Command docker -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }
    $desktop = "C:\Program Files\Docker\Docker\resources\bin\docker.exe"
    if (Test-Path $desktop) { return $desktop }
    throw @"
Docker introuvable. Installez Docker Desktop puis relancez :
  winget install Docker.DockerDesktop
https://docs.docker.com/desktop/setup/install/windows-install/
"@
}

$docker = Get-DockerExe

Push-Location $Root
try {
    switch ($Stage) {
        "test" {
            & $docker compose -f $ComposeFile run --rm --build ci-test
        }
        "package" {
            & $docker compose -f $ComposeFile --profile package run --rm --build ci-package
        }
        "all" {
            & $docker compose -f $ComposeFile run --rm --build ci-test
            if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
            & $docker compose -f $ComposeFile --profile package run --rm ci-package
        }
    }
    exit $LASTEXITCODE
} finally {
    Pop-Location
}

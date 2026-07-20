param(
    [string]$VersionName = "1.2.0"
)

$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
$releaseHome = Join-Path $env:USERPROFILE ".kai-board"
$localReleaseDir = Join-Path $projectRoot "Res"
$legacySigningDir = Join-Path $projectRoot ".signing"
$keyStorePath = if (Test-Path -LiteralPath (Join-Path $localReleaseDir "kai-board-release.jks")) {
    Join-Path $localReleaseDir "kai-board-release.jks"
} elseif (Test-Path -LiteralPath (Join-Path $legacySigningDir "kai-board-release.jks")) {
    Join-Path $legacySigningDir "kai-board-release.jks"
} else {
    Join-Path $releaseHome "kai-board-release.jks"
}
$secretPath = if (Test-Path -LiteralPath (Join-Path $localReleaseDir "signing-secret.xml")) {
    Join-Path $localReleaseDir "signing-secret.xml"
} elseif (Test-Path -LiteralPath (Join-Path $legacySigningDir "signing-secret.xml")) {
    Join-Path $legacySigningDir "signing-secret.xml"
} else {
    Join-Path $releaseHome "signing-secret.xml"
}

if (-not (Test-Path -LiteralPath $keyStorePath)) {
    throw "Khong tim thay khoa ky phat hanh: $keyStorePath"
}
if (-not (Test-Path -LiteralPath $secretPath)) {
    throw "Khong tim thay thong tin mo khoa: $secretPath"
}

$credential = Import-Clixml -LiteralPath $secretPath
$password = $credential.GetNetworkCredential().Password

$env:KAI_RELEASE_STORE_FILE = $keyStorePath
$env:KAI_RELEASE_STORE_PASSWORD = $password
$env:KAI_RELEASE_KEY_ALIAS = $credential.UserName
$env:KAI_RELEASE_KEY_PASSWORD = $password

try {
    Push-Location $projectRoot
    & .\gradlew.bat testDebugUnitTest assembleRelease
    if ($LASTEXITCODE -ne 0) {
        throw "Gradle release build failed with exit code $LASTEXITCODE"
    }

    $sourceApk = Join-Path $projectRoot "app\build\outputs\apk\release\app-arm64-v8a-release.apk"
    if (-not (Test-Path -LiteralPath $sourceApk)) {
        throw "Khong tim thay APK release: $sourceApk"
    }

    $outputDir = Join-Path $projectRoot "outputs"
    New-Item -ItemType Directory -Force -Path $outputDir | Out-Null
    $outputApk = Join-Path $outputDir "KAI-Board-$VersionName-release.apk"
    Copy-Item -LiteralPath $sourceApk -Destination $outputApk -Force

    $hash = (Get-FileHash -LiteralPath $outputApk -Algorithm SHA256).Hash.ToLowerInvariant()
    Set-Content -LiteralPath "$outputApk.sha256" -Value "$hash  $(Split-Path -Leaf $outputApk)" -Encoding ascii
    Write-Host "Release APK: $outputApk"
    Write-Host "SHA-256: $hash"
}
finally {
    Pop-Location
    Remove-Item Env:KAI_RELEASE_STORE_FILE -ErrorAction SilentlyContinue
    Remove-Item Env:KAI_RELEASE_STORE_PASSWORD -ErrorAction SilentlyContinue
    Remove-Item Env:KAI_RELEASE_KEY_ALIAS -ErrorAction SilentlyContinue
    Remove-Item Env:KAI_RELEASE_KEY_PASSWORD -ErrorAction SilentlyContinue
    $password = $null
}

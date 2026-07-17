param(
    [string]$Serial = "",
    [switch]$Clean,
    [switch]$WithTests
)

$ErrorActionPreference = "Stop"
$projectDir = Split-Path -Parent $PSScriptRoot
$adb = Join-Path $env:LOCALAPPDATA "Android\Sdk\platform-tools\adb.exe"
$javaHome = "C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot"
$apk = Join-Path $projectDir "app\build\outputs\apk\debug\app-arm64-v8a-debug.apk"
$outputApk = Join-Path $projectDir "outputs\KAI-Board-0.29.9-debug.apk"
$ime = "vn.kai.board/.ime.KaiBoardImeService"

if (-not (Test-Path -LiteralPath $adb)) { throw "ADB not found: $adb" }
if (-not (Test-Path -LiteralPath $javaHome)) { throw "JDK 17 not found: $javaHome" }

if (-not $Serial) {
    $devices = @(& $adb devices | Select-Object -Skip 1 | Where-Object { $_ -match "\sdevice$" })
    if ($devices.Count -ne 1) { throw "Connect exactly one authorized Android device or pass -Serial." }
    $Serial = ($devices[0] -split "\s+")[0]
}

$env:JAVA_HOME = $javaHome
$gradle = Join-Path $projectDir "gradlew.bat"
$tasks = @()
if ($Clean) { $tasks += "clean" }
if ($WithTests) { $tasks += "testDebugUnitTest" }
$tasks += "assembleDebug"

$timer = [System.Diagnostics.Stopwatch]::StartNew()
& $gradle @tasks
if ($LASTEXITCODE -ne 0) { throw "Gradle build failed." }

Copy-Item -LiteralPath $apk -Destination $outputApk -Force
& $adb -s $Serial install -r $outputApk
if ($LASTEXITCODE -ne 0) { throw "APK install failed." }

& $adb -s $Serial shell ime set $ime | Out-Null
& $adb -s $Serial shell am start -n "vn.kai.board/.MainActivity" | Out-Null
$timer.Stop()

Write-Host ("KAI Board ready on {0} in {1:n1}s" -f $Serial, $timer.Elapsed.TotalSeconds)

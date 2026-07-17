param(
    [string]$Name = "kai-board",
    [string]$Serial = ""
)

$ErrorActionPreference = "Stop"
$projectDir = Split-Path -Parent $PSScriptRoot
$adb = Join-Path $env:LOCALAPPDATA "Android\Sdk\platform-tools\adb.exe"
$target = Join-Path $projectDir ("work\{0}.png" -f $Name)
$remote = "/sdcard/$Name.png"

if (-not $Serial) {
    $devices = @(& $adb devices | Select-Object -Skip 1 | Where-Object { $_ -match "\sdevice$" })
    if ($devices.Count -ne 1) { throw "Connect exactly one device or pass -Serial." }
    $Serial = ($devices[0] -split "\s+")[0]
}

& $adb -s $Serial shell screencap -p $remote
& $adb -s $Serial pull $remote $target | Out-Null
& $adb -s $Serial shell rm $remote
Write-Host $target

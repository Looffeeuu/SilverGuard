$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$wrapperDir = Join-Path $root "gradle\wrapper"
$jarPath = Join-Path $wrapperDir "gradle-wrapper.jar"
$url = "https://raw.githubusercontent.com/gradle/gradle/v9.5.0/gradle/wrapper/gradle-wrapper.jar"

New-Item -ItemType Directory -Force -Path $wrapperDir | Out-Null

Write-Host "Downloading official Gradle 9.5.0 wrapper..."
Invoke-WebRequest -Uri $url -OutFile $jarPath

if (-not (Test-Path $jarPath)) {
    throw "gradle-wrapper.jar download failed."
}

Write-Host ""
Write-Host "Done. Now open this folder in Android Studio."
Write-Host "You can also run: .\gradlew.bat --version"

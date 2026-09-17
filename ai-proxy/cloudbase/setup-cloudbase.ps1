param(
    [Parameter(Mandatory = $true)]
    [ValidatePattern("^[a-z0-9-]{1,64}$")]
    [string]$EnvironmentId,
    [switch]$ValidateOnly,
    [switch]$ConfigureRouteOnly,
    [switch]$KeepWindowOpen
)

$ErrorActionPreference = "Stop"

function Set-SilverGuardHttpRoute {
    # CLI 3.8.1 fn deploy --path creates an SCF route even for an HTTP function.
    $routeConfig = @{
        domain = '*'
        routes = @(@{
            path = '/silverguard-ai'
            upstreamResourceType = 'WEB_SCF'
            enablePathTransmission = $true
        })
    } | ConvertTo-Json -Depth 5 -Compress
    # npx.cmd uses legacy Windows argument passing; retain the JSON quotes.
    $routeArgument = $routeConfig.Replace('"', '\"')
    'y' | & npx.cmd --yes --package @cloudbase/cli@3.8.1 tcb -e $EnvironmentId -r ap-shanghai `
        routes edit --data $routeArgument --json
    if ($LASTEXITCODE -ne 0) {
        throw "CloudBase HTTP route configuration failed with exit code $LASTEXITCODE."
    }
}

function Invoke-SilverGuardDeployment {

$cloudBaseRoot = $PSScriptRoot
$projectRoot = Resolve-Path (Join-Path $cloudBaseRoot "..\..")
$functionDirectory = Join-Path $cloudBaseRoot "function"
$localPropertiesPath = Join-Path $projectRoot "local.properties"
$configPath = Join-Path $cloudBaseRoot "cloudbaserc.json"

if (Test-Path -LiteralPath $configPath) {
    throw "Refusing to overwrite the existing local CloudBase configuration: $configPath"
}
if (-not (Test-Path -LiteralPath $localPropertiesPath)) {
    throw "Missing local.properties. Build the Android project once and configure the local AI client token first."
}

$tokenLine = Get-Content -LiteralPath $localPropertiesPath -Encoding UTF8 |
    Where-Object { $_ -match '^\s*SILVERGUARD_AI_PROXY_TOKEN\s*=' } |
    Select-Object -Last 1
if ([string]::IsNullOrWhiteSpace($tokenLine)) {
    throw "SILVERGUARD_AI_PROXY_TOKEN is not configured in local.properties."
}
$clientToken = ($tokenLine -split '=', 2)[1].Trim()
if ([string]::IsNullOrWhiteSpace($clientToken)) {
    throw "The Android AI client token is empty."
}
if (-not (Get-Command npx.cmd -ErrorAction SilentlyContinue)) {
    throw "Node.js / npx is not available."
}
foreach ($requiredFile in @('index.js', 'package.json', 'scf_bootstrap')) {
    if (-not (Test-Path -LiteralPath (Join-Path $functionDirectory $requiredFile))) {
        throw "Missing CloudBase function file: $requiredFile"
    }
}
if ($ValidateOnly) {
    Write-Host "Preflight passed: the Android AI client token and deployment files are available."
    return
}

Write-Host "SilverGuard CloudBase deployment"
Write-Host "Environment: $EnvironmentId"
Write-Host "The API Key is used only in memory and a temporary ignored deployment file."
$secureApiKey = Read-Host -Prompt "Enter the rotated Zhipu API Key (hidden)" -AsSecureString
$keyPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secureApiKey)
try {
    $apiKey = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($keyPointer)
}
finally {
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($keyPointer)
}
if ([string]::IsNullOrWhiteSpace($apiKey)) {
    throw "The API Key cannot be empty."
}

$deploymentConfig = @{
    '$schema' = 'https://static.cloudbase.net/cli/cloudbaserc.schema.json'
    envId = $EnvironmentId
    functionRoot = 'function'
    functions = @(
        @{
            name = 'silverguard-ai-proxy'
            runtime = 'Nodejs20.19'
            handler = 'index.main'
            timeout = 3
            memorySize = 256
            description = 'SilverGuard AI development proxy'
            envVariables = @{
                ZHIPU_API_KEY = $apiKey
                SILVERGUARD_APP_TOKEN = $clientToken
                SILVERGUARD_UPSTREAM_TIMEOUT_MS = '2500'
            }
        }
    )
} | ConvertTo-Json -Depth 8

try {
    [System.IO.File]::WriteAllText($configPath, $deploymentConfig, [System.Text.UTF8Encoding]::new($false))
    Push-Location $cloudBaseRoot
    try {
        & npx.cmd --yes --package @cloudbase/cli@3.8.1 tcb -e $EnvironmentId -r ap-shanghai fn deploy silverguard-ai-proxy `
            --dir $functionDirectory --httpFn --path /silverguard-ai --runtime Nodejs20.19 `
            --install-dependency false --force
        if ($LASTEXITCODE -ne 0) {
            throw "CloudBase deployment failed with exit code $LASTEXITCODE."
        }
        Set-SilverGuardHttpRoute
    }
    finally {
        Pop-Location
    }
}
finally {
    if (Test-Path -LiteralPath $configPath) {
        Remove-Item -LiteralPath $configPath -Force
    }
    $apiKey = $null
    $clientToken = $null
    $deploymentConfig = $null
    $secureApiKey.Dispose()
    [GC]::Collect()
}

Write-Host "Deployment completed. You can close this window and return to Codex."
}

$deploymentExitCode = 0
try {
    if ($ConfigureRouteOnly) {
        Set-SilverGuardHttpRoute
    }
    else {
        Invoke-SilverGuardDeployment
    }
}
catch {
    $deploymentExitCode = 1
    Write-Host "Deployment stopped. Return to Codex with the error below." -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
}
finally {
    if ($KeepWindowOpen) {
        # Keep the result visible without returning to an interactive command prompt.
        $closeInput = Read-Host -Prompt "Press Enter to close this window" -AsSecureString
        $closeInput.Dispose()
    }
}
exit $deploymentExitCode

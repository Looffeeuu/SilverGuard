$ErrorActionPreference = 'Stop'

function ConvertFrom-SecureValue([Security.SecureString]$SecureValue) {
    $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($SecureValue)
    try {
        [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer)
    } finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer)
    }
}

Write-Host 'SilverGuard AI 安全代理配置' -ForegroundColor Green
Write-Host 'API Key 只会写入临时文件并上传为 Cloudflare Secret，不会显示在屏幕或写入项目。'
$secureApiKey = Read-Host '请粘贴智谱 API Key，然后按回车' -AsSecureString
$apiKey = ConvertFrom-SecureValue $secureApiKey
if ([string]::IsNullOrWhiteSpace($apiKey) -or $apiKey.Length -lt 12) {
    throw '没有读取到有效的智谱 API Key。'
}

$tokenBytes = [byte[]]::new(32)
[Security.Cryptography.RandomNumberGenerator]::Fill($tokenBytes)
$appToken = [Convert]::ToHexString($tokenBytes).ToLowerInvariant()
$secretFile = Join-Path ([IO.Path]::GetTempPath()) ("silverguard-secrets-{0}.json" -f [Guid]::NewGuid())
$pushedLocation = $false

try {
    $secretJson = @{
        ZHIPU_API_KEY = $apiKey
        SILVERGUARD_APP_TOKEN = $appToken
    } | ConvertTo-Json
    [IO.File]::WriteAllText($secretFile, $secretJson, [Text.UTF8Encoding]::new($false))
    $apiKey = $null

    Push-Location $PSScriptRoot
    $pushedLocation = $true
    $deployOutput = & npx wrangler@latest deploy --secrets-file $secretFile 2>&1
    $deployExit = $LASTEXITCODE
    $deployOutput | ForEach-Object { Write-Host $_ }
    if ($deployExit -ne 0) {
        throw "Cloudflare 部署失败，退出码：$deployExit"
    }

    $outputText = $deployOutput -join "`n"
    $urlMatch = [regex]::Match($outputText, 'https://[A-Za-z0-9.-]+\.workers\.dev')
    if (-not $urlMatch.Success) {
        throw '代理已部署，但没有自动识别到 workers.dev 地址。请从上方输出复制地址并手动配置 local.properties。'
    }
    $proxyUrl = $urlMatch.Value.TrimEnd('/') + '/v1/chat/completions'

    $localPropertiesPath = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\local.properties'))
    $projectRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
    if (-not $localPropertiesPath.StartsWith($projectRoot, [StringComparison]::OrdinalIgnoreCase)) {
        throw 'local.properties 路径不在 SilverGuard 项目内，已停止写入。'
    }
    $existing = if (Test-Path -LiteralPath $localPropertiesPath) {
        [IO.File]::ReadAllText($localPropertiesPath, [Text.Encoding]::UTF8)
    } else {
        ''
    }
    $withoutAiConfig = [regex]::Replace(
        $existing,
        '(?m)^SILVERGUARD_AI_PROXY_(?:URL|TOKEN)=.*(?:\r?\n|$)',
        ''
    ).TrimEnd()
    $updated = $withoutAiConfig + "`r`n" +
        "SILVERGUARD_AI_PROXY_URL=$proxyUrl`r`n" +
        "SILVERGUARD_AI_PROXY_TOKEN=$appToken`r`n"
    [IO.File]::WriteAllText($localPropertiesPath, $updated, [Text.UTF8Encoding]::new($false))

    Write-Host ''
    Write-Host '部署和本机配置完成。请回到 Android Studio 重新 Sync / Build。' -ForegroundColor Green
    Write-Host "代理地址：$proxyUrl"
} finally {
    $apiKey = $null
    if ($pushedLocation) { Pop-Location }
    if (Test-Path -LiteralPath $secretFile) {
        Remove-Item -LiteralPath $secretFile -Force
    }
}

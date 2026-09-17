$ErrorActionPreference = 'Stop'
$env:NO_COLOR = '1'
$env:FORCE_COLOR = '0'
[Console]::InputEncoding = [Text.UTF8Encoding]::new($false)
[Console]::OutputEncoding = [Text.UTF8Encoding]::new($false)
$OutputEncoding = [Text.UTF8Encoding]::new($false)

function ConvertFrom-SecureValue([Security.SecureString]$SecureValue) {
    $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($SecureValue)
    try {
        [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer)
    } finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer)
    }
}

function Initialize-CompatibleEdgeOneCli {
    $cliRoot = Join-Path ([IO.Path]::GetTempPath()) 'silverguard-edgeone-cli'
    $cliPackageRoot = Join-Path $cliRoot 'node_modules\edgeone'
    $cliBin = Join-Path $cliPackageRoot 'edgeone-bin\edgeone.js'
    $cliSource = Join-Path $cliPackageRoot 'edgeone-dist\cli.js'

    if (-not (Test-Path -LiteralPath $cliSource)) {
        Write-Host '正在准备 EdgeOne 官方部署工具…'
        & npm install `
            --prefix $cliRoot `
            edgeone@1.6.32 `
            --ignore-scripts `
            --no-audit `
            --no-fund | Out-Null
        if ($LASTEXITCODE -ne 0) {
            throw 'EdgeOne 官方部署工具下载失败。'
        }
    }

    # EdgeOne CLI 1.6.32 的 env 子命令没有返回异步 Promise，导致进程在
    # 环境变量请求完成前退出。这里只修正系统临时目录中的私有副本。
    $cliText = [IO.File]::ReadAllText($cliSource, [Text.Encoding]::UTF8)
    foreach ($commandName in @('pull', 'ls', 'set', 'rm')) {
        $brokenHandler = 't=>{JY("' + $commandName + '",t)}'
        $fixedHandler = 't=>JY("' + $commandName + '",t)'
        if ($cliText.Contains($brokenHandler)) {
            $cliText = $cliText.Replace($brokenHandler, $fixedHandler)
        } elseif (-not $cliText.Contains($fixedHandler)) {
            throw "无法确认 EdgeOne CLI 的 $commandName 兼容状态。"
        }
    }
    [IO.File]::WriteAllText($cliSource, $cliText, [Text.UTF8Encoding]::new($false))
    $script:EdgeOneCliBin = $cliBin
}

function Invoke-EdgeOneCommand([string[]]$Arguments, [bool]$ShowOutput = $true) {
    $commandOutput = & node $script:EdgeOneCliBin @Arguments 2>&1
    $commandExit = $LASTEXITCODE
    if ($ShowOutput) {
        $commandOutput | ForEach-Object { Write-Host $_ }
    }
    if ($commandExit -ne 0) {
        throw "EdgeOne 命令执行失败，退出码：$commandExit"
    }
    $commandOutput
}

Write-Host 'SilverGuard EdgeOne 国内 AI 代理配置' -ForegroundColor Green
Write-Host 'API Key 使用隐藏输入，只上传到 EdgeOne 服务端环境变量，不会写入项目。'
Initialize-CompatibleEdgeOneCli
$secureApiKey = Read-Host '请粘贴新创建的智谱 API Key，然后按回车' -AsSecureString
$apiKey = ConvertFrom-SecureValue $secureApiKey
if ([string]::IsNullOrWhiteSpace($apiKey) -or $apiKey.Length -lt 12) {
    throw '没有读取到有效的智谱 API Key。'
}

$projectRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$localPropertiesPath = [IO.Path]::GetFullPath((Join-Path $projectRoot 'local.properties'))
if (-not $localPropertiesPath.StartsWith($projectRoot, [StringComparison]::OrdinalIgnoreCase)) {
    throw 'local.properties 路径不在 SilverGuard 项目内，已停止写入。'
}

$existing = if (Test-Path -LiteralPath $localPropertiesPath) {
    [IO.File]::ReadAllText($localPropertiesPath, [Text.Encoding]::UTF8)
} else {
    ''
}
$existingTokenMatch = [regex]::Match($existing, '(?m)^SILVERGUARD_AI_PROXY_TOKEN=(.+)$')
if ($existingTokenMatch.Success -and $existingTokenMatch.Groups[1].Value.Trim().Length -ge 32) {
    $appToken = $existingTokenMatch.Groups[1].Value.Trim()
} else {
    $tokenBytes = [byte[]]::new(32)
    [Security.Cryptography.RandomNumberGenerator]::Fill($tokenBytes)
    $appToken = [Convert]::ToHexString($tokenBytes).ToLowerInvariant()
}

$edgeOneRoot = Join-Path $PSScriptRoot 'edgeone'
$pushedLocation = $false
try {
    Push-Location $edgeOneRoot
    $pushedLocation = $true

    Write-Host ''
    Write-Host '1/4 创建并上传 EdgeOne 项目…' -ForegroundColor Cyan
    $firstDeploy = Invoke-EdgeOneCommand @(
        'makers', 'deploy', '.',
        '--name', 'silverguard-ai-proxy-cn',
        '--area', 'global',
        '--json'
    )

    Write-Host '2/4 关联项目并写入服务端配置…' -ForegroundColor Cyan
    Invoke-EdgeOneCommand @('makers', 'link', '--name', 'silverguard-ai-proxy-cn') $false | Out-Null
    Invoke-EdgeOneCommand @('makers', 'env', 'set', 'ZHIPU_API_KEY', $apiKey) $false | Out-Null
    Invoke-EdgeOneCommand @('makers', 'env', 'set', 'SILVERGUARD_APP_TOKEN', $appToken) $false | Out-Null
    $apiKey = $null

    Write-Host '3/4 让云端配置生效并验证真实调用…' -ForegroundColor Cyan
    $finalDeploy = Invoke-EdgeOneCommand @(
        'makers', 'deploy', '.',
        '--name', 'silverguard-ai-proxy-cn',
        '--area', 'global',
        '--json'
    )
    $deployText = ($finalDeploy -join "`n")
    $urlMatches = [regex]::Matches(
        $deployText,
        'https://[A-Za-z0-9.-]+\.edgeone\.(?:app|site|cool)(?:\?[^\s"'']+)?'
    )
    if ($urlMatches.Count -eq 0) {
        throw '部署成功，但没有自动识别到 EdgeOne 访问地址。'
    }
    $deployUrl = [Uri]$urlMatches[$urlMatches.Count - 1].Value.TrimEnd('/')
    $cleanBaseBuilder = [UriBuilder]$deployUrl
    $cleanBaseBuilder.Path = '/'
    $cleanBaseBuilder.Query = ''
    $endpointBuilder = [UriBuilder]$cleanBaseBuilder.Uri
    $endpointBuilder.Path = '/health'
    $healthUrl = $endpointBuilder.Uri.AbsoluteUri
    $endpointBuilder.Path = '/chat'
    $proxyUrl = $endpointBuilder.Uri.AbsoluteUri

    $sessionArguments = @{}
    if ($deployUrl.Host.EndsWith('.edgeone.cool', [StringComparison]::OrdinalIgnoreCase)) {
        $null = Invoke-WebRequest `
            -Method Get `
            -Uri $deployUrl.AbsoluteUri `
            -SessionVariable edgeOnePreviewSession `
            -TimeoutSec 30
        $sessionArguments.WebSession = $edgeOnePreviewSession
    }

    $health = Invoke-RestMethod `
        -Method Get `
        -Uri $healthUrl `
        -TimeoutSec 30 `
        @sessionArguments
    if ($health.status -ne 'ok' -or $health.service -ne 'edgeone') {
        throw 'EdgeOne 健康检查返回了非预期结果。'
    }

    $sampleBody = @{
        messages = @(
            @{ role = 'system'; content = '只返回 JSON：{"summary":"一句简短中文结论"}' },
            @{ role = 'user'; content = '测试商品宣传：七天根治高血压，不用吃药。' }
        )
    } | ConvertTo-Json -Depth 5 -Compress
    $liveResult = Invoke-RestMethod `
        -Method Post `
        -Uri $proxyUrl `
        -Headers @{ 'X-SilverGuard-Client-Token' = $appToken } `
        -ContentType 'application/json; charset=utf-8' `
        -Body $sampleBody `
        -TimeoutSec 60 `
        @sessionArguments
    if (-not $liveResult.choices -or $liveResult.choices.Count -lt 1) {
        throw '代理已连通，但真实 AI 响应缺少 choices，暂不切换 Android 配置。'
    }

    Write-Host '4/4 更新 Android 本机配置…' -ForegroundColor Cyan
    $withoutAiConfig = [regex]::Replace(
        $existing,
        '(?m)^SILVERGUARD_AI_PROXY_(?:URL|TOKEN)=.*(?:\r?\n|$)',
        ''
    ).TrimEnd()
    $updated = $withoutAiConfig + "`r`n" +
        "SILVERGUARD_AI_PROXY_URL=$proxyUrl`r`n" +
        "SILVERGUARD_AI_PROXY_TOKEN=$appToken`r`n"
    if ($deployUrl.Host.EndsWith('.edgeone.cool', [StringComparison]::OrdinalIgnoreCase)) {
        Write-Warning 'EdgeOne 当前只返回短期预览地址；已完成真实调用验证，但不会覆盖 Android 的长期代理配置。'
    } else {
        [IO.File]::WriteAllText($localPropertiesPath, $updated, [Text.UTF8Encoding]::new($false))
    }

    Write-Host ''
    Write-Host 'EdgeOne 部署和真实 AI 调用验证均已完成。' -ForegroundColor Green
    if (-not $deployUrl.Host.EndsWith('.edgeone.cool', [StringComparison]::OrdinalIgnoreCase)) {
        Write-Host 'Android 本机配置已切换到 EdgeOne。'
    }
} finally {
    $apiKey = $null
    $appToken = $null
    if ($pushedLocation) { Pop-Location }
}

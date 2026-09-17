# SilverGuard AI 安全代理

代理负责在服务端保存智谱 API Key。Android App 不直接持有智谱密钥。

## 国内网络推荐：腾讯 EdgeOne

首次使用先登录并开通 EdgeOne Makers：

```powershell
cd .\edgeone
npx --yes edgeone@latest login --site china --local
```

在腾讯云控制台完成免费开通后，回到本目录运行：

```powershell
pwsh -NoProfile -File .\setup-edgeone.ps1
```

脚本会隐藏读取智谱 API Key、创建国内代理、配置服务端环境变量、执行一次真实 AI 调用，并且只有验证成功后才更新 `local.properties`。EdgeOne 源码在 `edgeone/`，路由为 `/health` 和 `/chat`。

## 备用：Cloudflare Worker

### 推荐：一键安全配置

先在智谱官方控制台创建 API Key，然后运行：

```powershell
pwsh -NoProfile -File .\setup-and-deploy.ps1
```

脚本会用隐藏输入读取 API Key、生成独立的应用访问令牌、部署 Worker，并自动更新项目的 `local.properties`。API Key 不会显示在终端，也不会写入项目；仅在系统临时目录短暂存在，上传为 Cloudflare Secret 后立即删除。

### 手动部署

1. 注册或登录 Cloudflare。
2. 在本目录运行 `npx wrangler@latest login`。
3. 生成一段至少 32 字符的随机 `SILVERGUARD_APP_TOKEN`。
4. 分别运行 `npx wrangler@latest secret put ZHIPU_API_KEY` 和 `npx wrangler@latest secret put SILVERGUARD_APP_TOKEN`，按提示输入值。
5. 运行 `npm run deploy`，记下返回的 `https://...workers.dev` 地址。
6. 在 Android 项目的 `local.properties` 中配置：

```properties
SILVERGUARD_AI_PROXY_URL=https://你的地址.workers.dev/v1/chat/completions
SILVERGUARD_AI_PROXY_TOKEN=与云端相同的应用访问令牌
```

重新构建 APK 后即可真实调用。

`SILVERGUARD_APP_TOKEN` 只用于限制代理访问，不是智谱 API Key。它仍可能从公开分发的 APK 中被提取，因此公开发布前应进一步接入 Play Integrity、设备注册或用户级短期令牌。当前实现更适合内测和小范围试用。

代理不会记录请求正文；它固定使用 `glm-4.7-flash`，限制请求和响应大小，并通过 Cloudflare Rate Limiting binding 限制每个应用令牌每分钟的调用次数。

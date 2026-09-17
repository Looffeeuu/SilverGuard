# SilverGuard CloudBase AI 代理

这是供国内开发测试使用的 CloudBase HTTP 云函数。智谱 API Key 只配置为 CloudBase 云端环境变量，Android 端仅携带独立的应用访问令牌。

## 部署前提

1. 在 CloudBase 控制台创建免费体验环境，记录环境 ID。
2. 使用 `tcb login` 登录同一个腾讯云账号。
3. 在云函数环境变量中配置 `ZHIPU_API_KEY`、`SILVERGUARD_APP_TOKEN` 和 `SILVERGUARD_UPSTREAM_TIMEOUT_MS`。

可运行 `setup-cloudbase.ps1 -EnvironmentId <环境 ID> -KeepWindowOpen` 完成部署。脚本会在本机以隐藏输入方式读取智谱 Key，从 `local.properties` 的 `SILVERGUARD_AI_PROXY_TOKEN` 读取应用令牌，临时配置云端环境变量，部署结束后删除本地临时配置；密钥不会进入仓库。

部署前可用 `-ValidateOnly` 检查本机配置。部署脚本自动将 `/silverguard-ai` 路由设置为 `WEB_SCF` 并开启路径透传；已部署环境也可用 `-ConfigureRouteOnly` 单独修正路由，不需要再次输入 Key。

免费环境的云函数最长运行 3 秒且不支持修改，因此本配置只能用于部署和短请求验证，不能保证完成完整 AI 分析。代理在 2.5 秒后中止上游请求，超时返回 `503 / upstream_timeout`；期限包含读取响应正文。请求固定使用 `glm-4.7-flash` 并关闭默认思考模式，以减少延迟。模型返回的 429 会保留为 429，App 可提示稍后重试。

完整 AI 分析需要支持更长函数超时的运行环境；正式服务还需要处理默认域名的测试用途限制。在更换套餐前需由项目所有者确认，不能自动升级。

官方说明：[CloudBase 资源点套餐限制](https://cloud.tencent.com/document/product/876/127357)、[HTTP 路由配置](https://docs.cloudbase.net/cli-v1/gateway)、[智谱思考模式](https://docs.bigmodel.cn/cn/guide/capabilities/thinking-mode)。

路由：

- `GET /health`
- `POST /chat`

网关地址为 `https://<环境默认域名>/silverguard-ai/health` 和 `https://<环境默认域名>/silverguard-ai/chat`。

## 2026-09-06 部署验收记录

- 云函数部署成功，健康检查 HTTP 200，`configured: true`。
- 修正 CLI 自动创建的路由类型不匹配问题（SCF → WEB_SCF）。
- 本地代理测试 6/6 通过。
- 完整模拟分析首次在约 2.7 秒后超时。
- 关闭思考模式后的验证遇到上游 HTTP 429；最小直连诊断确认智谱错误码 1305，提示当前模型访问量过大。
- 尚未获得完整、可被 App 读取的真实分析结果，Android 的连接地址尚未切换到此环境。

健康检查通过仅说明代理进程和变量已配置，不代表模型已成功返回分析。只有真实分析通过后，才更新 Android 本地连接地址并构建 APK。

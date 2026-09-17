# SilverGuard 架构与关键决策

## 总览

SilverGuard 使用单 Activity + Jetpack Compose。业务规则与界面分离，核心分析可以在不启动 Android UI 的情况下通过 JVM 测试验证。

```text
Input
  ├─ camera / gallery → ML Kit OCR
  ├─ manual text
  └─ supported e-commerce URL → constrained public-page parser
        ↓
ProductInfoExtractor + EcommerceProduct mapper
        ↓
RiskAnalyzer + RegistrationNumberClassifier
        ↓
VerificationPlanner + ClaimRegistrationConflictAnalyzer
        ↓
Compose result UI / speech / share report / local history
```

## 分层

| 目录 | 职责 |
| --- | --- |
| `model/` | 商品、风险、注册编号、官方核验、报价和历史等领域模型 |
| `data/` | 官方来源配置、公开案例资料、应用私有历史存储 |
| `engine/` | 纯 Kotlin 提取器、分类器、风险规则、核验准备度与报告生成 |
| `network/` | 受限电商网页读取和默认关闭的可选 AI Provider |
| `parser/` | 淘宝 / 天猫公开静态信息解析，记录字段来源与置信度 |
| `ui/` | Compose 页面、卡片、主题、朗读与系统交互 |

## 关键决策

### 1. 本地规则是主流程

风险分数由可解释的本地规则产生。即使未来重新启用 AI，AI 结果也只作为独立补充，不覆盖本地分数、官方核验状态或购买建议。

### 2. “官方入口”不等于“官方验证”

没有稳定公开 API 时，App 只完成编号分类、核验准备度判断、官方入口导航、人工记录和官方网页截图辅助比对。只有真正取得官方数据结果后，才能进入相应的验证状态。

### 3. 所有外部链接先经过边界检查

电商解析限制为明确的 HTTPS Host 白名单、有限次数重定向、响应大小限制和超时控制。不携带 Cookie、登录令牌，不执行 JavaScript，也不绕过登录或验证码。

### 4. 历史是快照，不是重新查询

历史记录保存当时的分析对象、输入来源、核对记录和报价。打开历史不会重新访问电商或官方网页；界面会标出保存时间。用户主动重新分析时创建新快照，保留旧记录。

### 5. 风险提示与真假判断分离

价格偏离、风险话术、字段缺失和案例相似只能触发进一步核验建议，不能单独推导“假货”“违法”或“官方认证”。

## 测试策略

- 业务规则优先写成无 Android 依赖的纯 Kotlin 组件，并由 JVM 单元测试覆盖。
- Compose UI 测试覆盖首页入口、无障碍设置、结果折叠、报价录入、首页返回和历史恢复。
- 颜色对比度由测试计算，避免仅靠人工观察。
- GitHub Actions 运行 JVM 测试并构建 Debug APK；设备 UI 测试在本地模拟器执行。

## 可扩展方向

- 将 `SilverGuardApp.kt` 中的页面状态继续迁移到 ViewModel / reducer。
- 为历史格式增加显式 migration，而不是只支持当前 v1 envelope。
- 接入合法稳定的官方数据接口后，为自动结果增加来源、查询时间和原始证据快照。
- 发布前增加真实老人可用性测试、无障碍审计和正式隐私政策。

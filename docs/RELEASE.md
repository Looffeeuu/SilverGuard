# 发布构建

Release 构建开启了 R8 代码压缩、混淆和资源压缩（`isMinifyEnabled`、`isShrinkResources`）。签名信息只从本机 `local.properties` 或环境变量读取，**签名文件和密码不进入仓库**。

## 1. 生成签名文件（只需一次）

签名文件决定了以后能否给已安装的用户推送更新：丢失后无法再以同一身份更新应用，泄露后他人可以冒充发布。请自己生成，并放在仓库目录**之外**，例如：

```powershell
keytool -genkeypair -v -keystore D:\keys\silverguard-release.jks -alias silverguard -keyalg RSA -keysize 4096 -validity 10000
```

按提示设置密码和证书信息。生成后备份到安全的位置（例如密码管理器或离线存储）。仓库的 `.gitignore` 已忽略 `*.jks`、`*.keystore` 和 `local.properties`，但仍然不要把签名文件放进项目目录。

## 2. 配置签名

在项目根目录的 `local.properties` 中加入（或设置同名环境变量，适合 CI）：

```properties
SILVERGUARD_RELEASE_STORE_FILE=D:\\keys\\silverguard-release.jks
SILVERGUARD_RELEASE_STORE_PASSWORD=你的 keystore 密码
SILVERGUARD_RELEASE_KEY_ALIAS=silverguard
SILVERGUARD_RELEASE_KEY_PASSWORD=你的 key 密码
```

没有配置 `SILVERGUARD_RELEASE_STORE_FILE` 时，`assembleRelease` 仍会成功，但输出的是未签名的 `app-release-unsigned.apk`，不能直接安装。CI 就是这样构建的，用来提前发现 R8 配置问题。

## 3. 构建

```powershell
.\gradlew.bat testDebugUnitTest assembleRelease
```

输出位于 `app/build/outputs/apk/release/`，R8 的映射文件位于 `app/build/outputs/mapping/release/mapping.txt`。**每次发布都要保存对应的 `mapping.txt`**，否则无法还原线上崩溃堆栈。

## 4. 发布前在真机或模拟器上检查

R8 的问题通常只在运行时出现，单元测试和 Debug 构建都发现不了。安装 release 包后至少检查：

- [ ] 首页、示例分析、结果页正常显示
- [ ] 分析后返回首页，**完全关闭应用再打开**，历史记录能打开（验证 Gson 读取历史）
- [ ] 选择一张带中文的截图，能识别出文字（验证 ML Kit OCR）
- [ ] `adb logcat` 中没有 `NoSuchMethodException`、`ClassNotFoundException`、`JsonSyntaxException`
- [ ] 如果是升级发布：先装旧版本并产生历史记录，再覆盖安装新版本，历史记录仍能打开

## R8 保留规则说明

规则在 `app/proguard-rules.pro`，目前有两类：

| 规则 | 原因 |
| --- | --- |
| 保留 `com.silverguard.app.model.**` 和 `AnalysisHistoryCodec$Envelope` 的全部字段 | 本机历史用 Gson 按字段名保存为 JSON。字段被改名或删除后，新版本将读不出旧版本保存的历史 |
| 保留 `ComponentRegistrar` 实现类的无参构造函数 | ML Kit 在清单中登记组件并通过反射创建。R8 full mode 默认会删掉无参构造函数，导致 release 包里 OCR 失效，而 Debug 包正常 |

新增使用反射、Gson 或序列化的代码时，需要同步补充保留规则，并按上面的清单重新检查。

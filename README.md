# 三事

今天最多记 3 件最重要的事。勾完有一条轻量完成提示。数据只存在这台手机上，过了当天再打开，昨天的列表会被清空，不能回看。

不做云同步、桌面小组件、通知、账号和历史。

- 包名 `com.zts.sanshi`
- versionName `0.1.0` / versionCode `1`
- minSdk 26，compileSdk / targetSdk 37
- Kotlin + Jetpack Compose + Material 3，主色 `#2563EB`

## 本地构建

需要 JDK 17 和 Android SDK Platform 37.0、Build-Tools 36.0.0。

```bash
export ANDROID_HOME="$HOME/android-sdk"
./gradlew testDebugUnitTest assembleDebug
```

Debug APK：`app/build/outputs/apk/debug/app-debug.apk`。这是没有原生库拆分的 universal 包，可以直接装到 arm64 真机。签名是调试证书。

```bash
sha256sum app/build/outputs/apk/debug/app-debug.apk
```

## 测试

业务逻辑用 JVM 单元测试，界面分支用 Robolectric + Compose 语义测试，都在 `app/src/test`：

```bash
./gradlew testDebugUnitTest
```

覆盖这些行为：

- DataStore 最多 3 条，空内容不能保存
- 同一天重新打开仓库，条目还在
- 日期变化后刷新，列表被清空，新的一条记在今天
- 已经全部完成时再进页面，不增加庆祝动画代数；当次从未完成勾到全部完成才会加一代
- 空态、满员提示、添加按钮禁用、庆祝条、删除确认、编辑框空内容不能保存

Debug 覆盖率：

```bash
./gradlew createDebugUnitTestCoverageReport
```

报告在 `app/build/reports/coverage/test/debug/`。

没有仪器截图和端到端设备测试。真机安装由预发布 APK 验收。

## 持续集成

`.github/workflows/android.yml` 在 Ubuntu 上用 `android-actions/setup-android@v4`，`packages` 留空，不安装已经下架的 `tools` 包。流水线跑单元测试、`assembleDebug`，计算 APK 的 SHA-256，并在推到 `main` 时更新 prerelease `v0.1.0`。

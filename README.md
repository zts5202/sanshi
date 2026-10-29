# 三事

今天最多记 3 件最重要的事。v0.2.0 增加待办池、跨日未完成处置、循环任务、记录/打卡、本地提醒与桌面 2×2 小组件。数据只存在这台手机上，无云同步与账号。

- 包名 `com.zts.sanshi`
- versionName `0.2.0` / versionCode `2`
- minSdk 26，compileSdk / targetSdk 37
- Kotlin + Jetpack Compose + Material 3，主色 `#2563EB`

## 本地构建

需要 JDK 17 和 Android SDK Platform 37.0、Build-Tools 36+。

```bash
. /home/box/tools/env.sh   # 或自行 export JAVA_HOME / ANDROID_HOME
./gradlew testDebugUnitTest assembleDebug
```

Debug APK：`app/build/outputs/apk/debug/app-debug.apk`。

```bash
sha256sum app/build/outputs/apk/debug/app-debug.apk
```

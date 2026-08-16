# 构建是语输入法 APK

## 环境要求

- **Android SDK**：compileSdk 35，安装对应 Platform
- **NDK**：30.0.14904198（通过 SDK Manager 安装）
- **CMake**：3.22.1+（通过 SDK Manager 安装）
- **Java**：JDK 17（可使用 Android Studio 自带的 JBR）
- **Gradle**：8.11.1（wrapper 自动下载）

## 项目结构

```
SimeApp/
├── require/              # 固定版本的源码依赖（Git submodule）
│   ├── Sime/             # C++ 引擎
│   ├── Handwritten/      # 手写识别 runtime
│   └── ncnn/             # CPU 推理框架
└── Android/              # Android 项目根目录
    ├── gradlew
    ├── app/
    │   ├── build.gradle.kts
    │   └── src/main/
    │       ├── java/com/isma/sime/    # Java 代码
    │       ├── jni/                    # JNI 桥接 + CMakeLists.txt
    │       ├── assets/                 # 模型文件（见下方）
    │       └── res/                    # 布局、颜色、图标等资源
    └── local.properties               # SDK 路径（不入库）
```

首次检出后初始化固定版本的源码依赖：

```bash
git submodule update --init --recursive
```

## 准备工作

### 1. 配置 SDK 路径

在 `android/` 目录下创建 `local.properties`：

```properties
sdk.dir=/path/to/your/Android/Sdk
```

### 2. 准备模型文件

将以下文件放到 `android/app/src/main/assets/`：

| 文件 | 说明 |
|------|------|
| `sime.trie` | 拼音→汉字 Trie 词典 |
| `sime.cnt`  | 语言模型（候选排序） |

这些文件由 Sime 引擎仓库的 `pipeline/` 生成，不在 Git 仓库中。

## 构建

所有命令在 `Android/` 目录下执行。默认优先使用 `../require/` 中固定版本的 Sime、
Handwritten runtime 和 ncnn；未初始化 submodule 时，Sime/Handwritten 可回退到同级
源码目录。可以通过 `SIME_ENGINE_ROOT`、`HANDWRITTEN_ROOT` 或
`-PhandwrittenRoot=...` 覆盖。

### 不含手写模型的构建

传入 `-PincludeHandwriting=false` 会移除手写入口及其 NCNN 模型；手写 runtime 源码
仍会参与编译，但不会加载模型。此变体用于模型再分发权未确认的发行渠道：

```bash
./gradlew assembleRelease -PincludeHandwriting=false
```

### Debug 构建 + 安装到设备

```bash
export JAVA_HOME=/opt/android-studio/jbr   # 或你的 JDK 17 路径
./gradlew installDebug
```

APK 输出位置：`app/build/outputs/apk/debug/app-debug.apk`

### Release 构建

```bash
./gradlew assembleRelease
```

APK 输出位置：`app/build/outputs/apk/release/app-release-unsigned.apk`

> 注意：Release 包未签名，需要签名后才能安装。见下方「签名与发布」。

## 签名与发布

### 1. 生成签名密钥（只需一次）

```bash
keytool -genkey -v -keystore sime-release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias sime
```

按提示设置密码和信息。生成的 `sime-release.jks` 务必妥善保管，丢失后无法更新已发布的 app。

> 不要将 `.jks` 文件和密码提交到 Git 仓库。

### 2. 配置 Gradle 签名

在 `app/build.gradle.kts` 的 `android {}` 块中添加：

```kotlin
signingConfigs {
    create("release") {
        storeFile = file("../sime-release.jks")
        storePassword = "你的密码"
        keyAlias = "sime"
        keyPassword = "你的密码"
    }
}

buildTypes {
    release {
        isMinifyEnabled = false
        signingConfig = signingConfigs.getByName("release")
    }
}
```

也可以用环境变量代替硬编码密码：

```kotlin
storePassword = System.getenv("SIME_STORE_PASSWORD")
keyPassword = System.getenv("SIME_KEY_PASSWORD")
```

### 3. 构建签名 APK

```bash
./gradlew assembleRelease
```

输出：`app/build/outputs/apk/release/app-release.apk`（已签名，可直接安装）

### 4. 发布到 GitHub Release

```bash
gh release create v0.1.0 app/build/outputs/apk/release/app-release.apk \
    --title "v0.1.0" \
    --notes "首次发布"
```

## 支持的 ABI

当前配置构建两种架构：

- `arm64-v8a`：绝大多数手机
- `x86_64`：模拟器

如需修改，编辑 `app/build.gradle.kts` 中的 `abiFilters`。

## 常见问题

**Q: 提示 JAVA_HOME 未设置**
设置环境变量指向 JDK 17，例如 Android Studio 自带的：
```bash
export JAVA_HOME=/opt/android-studio/jbr
```

**Q: NDK 或 CMake 找不到**
通过 Android Studio → SDK Manager → SDK Tools 安装对应版本，或命令行：
```bash
$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager "ndk;30.0.14904198" "cmake;3.22.1"
```

**Q: assets 目录下缺少模型文件**
需要先在 Sime 引擎仓库运行训练 pipeline，生成 `sime.dict` 和 `sime.cnt`
后复制到 assets。

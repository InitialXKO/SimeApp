# 是语输入法应用

SimeApp 包含是语输入法的 Android、macOS 和 Fcitx5 前端。C++ 输入引擎、CLI
工具及模型训练流水线位于独立的
[Sime](https://github.com/Ismantic/Sime) 仓库。

## 目录

- `Android/`：Android IME、JNI 适配层及 JUnit 测试。
- `macOS/`：InputMethodKit 前端、Swift UI 和安装包脚本。
- `Linux/fcitx5/`：Fcitx5 插件。
- `Linux/package/`：Arch Linux 打包文件。
- `cmake/`：定位并引入 Sime 引擎的共享 CMake 模块。

## 获取源码

开发时推荐把两个仓库放在同一目录：

```text
Shiyu/
├── Sime/
└── SimeApp/
```

默认构建会从 `../Sime` 加载引擎。其他布局可设置环境变量
`SIME_ENGINE_ROOT`，或向 CMake 传入
`-DSIME_ENGINE_ROOT=/path/to/Sime`。

## 构建

Android：

```bash
cd Android
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

Fcitx5：

```bash
cmake -S Linux/fcitx5 -B build/fcitx5 \
  -DCMAKE_BUILD_TYPE=Release
cmake --build build/fcitx5
```

macOS：

```bash
cmake -S macOS -B build/macos -G Xcode
cmake --build build/macos --config Release
```

运行时模型 `sime.dict` 和 `sime.cnt` 由 Sime 的 `pipeline/` 生成，不提交到
本仓库。平台打包前需将模型复制到对应资源目录。

## 隐私

所有输入和用户数据仅在本机处理。完整说明见 [PRIVACY.md](PRIVACY.md)。

## License

[Apache-2.0](LICENSE)

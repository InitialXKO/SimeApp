# 是语输入法应用

SimeApp 包含是语输入法的 Android、macOS 和 Fcitx5 前端。C++ 输入引擎、CLI
工具及模型训练流水线位于独立的
[Sime](https://github.com/Ismantic/Sime) 仓库。

## 安装

- Android 和 macOS 的正式版本可以直接去 [Release 页面](https://github.com/Ismantic/SimeApp/releases) 下载。

## 0.16.0

- 新增本地 GRU 整句排序器，提升全拼和九宫格整句准确率；模型完全离线运行。
- 保留此前的九宫格解码延迟优化，GRU 使用单线程 ncnn CPU 推理。
- Arch Linux 可以直接安装 [AUR](https://aur.archlinux.org/packages/fcitx5-sime) 包。
- 开发或自行编译时，请从源码构建。

### Arch Linux

使用 `yay` 安装：

```bash
yay -S fcitx5-sime
```

安装完成后重启 Fcitx5：

```bash
fcitx5 -r
```

打开 `fcitx5-configtool`，搜索并添加“Sime”，随后即可切换使用。后续版本会随
常规的 `yay -Syu` 一起更新。

## 目录

- `Android/`：Android IME、JNI 适配层及 JUnit 测试。
- `macOS/`：InputMethodKit 前端、Swift UI 和安装包脚本。
- `Linux/fcitx5/`：Fcitx5 插件。
- `Linux/package/`：Arch Linux 打包文件。
- `cmake/`：定位并引入 Sime 引擎的共享 CMake 模块。

## 源码

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

如果你想自己编译或修改代码，可以从源码构建。

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

运行时模型 `sime.dict` 和 `sime.cnt` 由 Sime 的 `pipeline/` 生成，不提交到本仓库。平台打包前需将模型复制到对应资源目录。

## 隐私

所有输入和用户数据仅在本机处理。完整说明见 [PRIVACY.md](PRIVACY.md)。

## License

[Apache-2.0](LICENSE)

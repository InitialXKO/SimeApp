# Fcitx5 Sime 插件

将 Sime 拼音引擎集成到 Fcitx5 输入法框架。

## 打包安装 (Arch Linux)

```bash
cd Linux/package
makepkg -si
```

## 手动编译安装

```bash
cd SimeApp
cmake -S Linux/fcitx5 -B build/fcitx5 -DCMAKE_BUILD_TYPE=Release
cmake --build build/fcitx5
sudo cmake --install build/fcitx5
fcitx5 -r  # 重启 fcitx5
```

默认从兄弟目录 `../Sime` 加载引擎。其他目录布局请设置
`SIME_ENGINE_ROOT=/path/to/Sime`。

## 编译依赖

- fcitx5 >= 5.0
- extra-cmake-modules

## 使用

1. 在 fcitx5-configtool 中添加 "Sime" 输入法
2. 切换到 Sime，输入拼音，数字键或空格选词


# Android 桌面互动小宠物

一个可爱的安卓桌面悬浮窗宠物应用，支持触摸交互、心情系统、随机动作。

## 功能特性

- 🐱 **自定义宠物角色**：用 Canvas 绘制的可爱小宠物
- 👆 **触摸反馈**：点击宠物有反应（开心、生气、卖萌）
- 😊 **心情系统**：根据互动改变心情状态
- 🚶 **随机动作**：自动走动、卖萌、嘟嘴、打招呼
- 🎨 **可定制外观**：支持自定义颜色和配件

## 技术栈

- **Kotlin** - 现代 Android 开发语言
- **Jetpack Compose** - 声明式 UI 框架
- **Canvas API** - 自定义绘制宠物
- **Service + WindowManager** - 实现桌面悬浮窗

## 安装方式

### 方式一：直接安装 APK（推荐）
1. 下载 `app/release/app-release.apk`
2. 传输到安卓手机
3. 安装并授予"显示在其他应用上层"权限
4. 打开应用，点击"启动宠物"

### 方式二：从源码构建
```bash
# 1. 安装 Android Studio
# 2. 打开项目：File > Open > DesktopPet
# 3. 连接手机或启动模拟器
# 4. 点击 Run 按钮
```

## 项目结构

```
DesktopPet/
├── app/
│   ├── src/main/java/com/example/desktoppet/
│   │   ├── MainActivity.kt          # 主界面
│   │   ├── PetService.kt            # 悬浮窗服务
│   │   ├── PetView.kt               # 宠物绘制与动画
│   │   ├── PetState.kt              # 宠物状态管理
│   │   └── PetConfig.kt             # 配置管理
│   └── build.gradle.kts
└── README.md
```

## 使用说明

1. **启动宠物**：打开应用 → 点击"启动宠物" → 授予权限
2. **交互**：
   - 点击宠物：随机反应（开心、卖萌、打招呼）
   - 长按拖拽：移动位置
   - 长时间不理：宠物会生气或嘟嘴
3. **关闭**：打开应用 → 点击"关闭宠物"

## 权限说明

- `SYSTEM_ALERT_WINDOW`：显示桌面悬浮窗（必需）
- `FOREGROUND_SERVICE`：后台运行服务（必需）
- `POST_NOTIFICATIONS`：通知栏提示（可选）

## 自定义宠物

编辑 `PetView.kt` 中的 `drawPet()` 方法，可以：
- 修改身体颜色
- 添加配件（帽子、领结、眼镜等）
- 设计新的表情和动作

## 开发环境

- Android Studio Hedgehog 或更高版本
- Kotlin 1.9+
- Compile SDK 34
- Min SDK 24 (Android 7.0)
- Target SDK 34

## License

MIT License - 自由使用和修改

# Desktop Pet - 桌面互动小宠物 APK 构建指南

## 快速开始（无需开发环境）

### 方法 1: 在线构建服务
使用在线 Android 构建服务，无需安装 Android Studio：

1. **GitHub Actions 自动构建**
   - 上传代码到 GitHub 仓库
   - 启用 GitHub Actions
   - 自动生成 APK 下载

2. **在线编译服务**
   - https://appcircle.io/ (免费额度)
   - https://bitrise.io/ (免费额度)
   - 上传项目 → 配置 → 构建 → 下载 APK

### 方法 2: 本地构建（需要 Android Studio）

#### 环境准备
1. 下载并安装 [Android Studio](https://developer.android.com/studio) (Hedgehog 或更高版本)
2. 安装 JDK 17 或更高版本
3. 配置 Android SDK

#### 构建步骤
```bash
# 1. 打开 Android Studio
# 2. File > Open > 选择 DesktopPet 文件夹
# 3. 等待 Gradle 同步完成
# 4. Build > Build Bundle(s) / APK(s) > Build APK(s)
# 5. APK 位于: app/build/outputs/apk/debug/app-debug.apk
```

#### 命令行构建（无需打开 IDE）
```powershell
# Windows PowerShell
cd DesktopPet
.\gradlew.bat assembleDebug

# APK 位置: app\build\outputs\apk\debug\app-debug.apk
```

## 安装 APK 到手机

### 方式 1: USB 传输
1. 复制 `app-debug.apk` 到手机存储
2. 打开文件管理器
3. 点击 APK 文件安装
4. 允许"安装未知应用"权限

### 方式 2: ADB 安装
```bash
# 连接手机，开启 USB 调试
adb install app-debug.apk
```

### 方式 3: 二维码下载
1. 将 APK 上传到云存储（百度网盘、阿里云盘等）
2. 生成分享链接
3. 手机扫描二维码下载

## 首次运行设置

### 必需权限
1. **悬浮窗权限**：设置 → 应用 → 桌面小宠物 → 权限 → 显示在其他应用上层 → 允许
2. **通知权限**（Android 13+）：设置 → 应用 → 桌面小宠物 → 权限 → 通知 → 允许

### 可选权限
- **开机自启动**：设置 → 应用 → 桌面小宠物 → 开机自启动 → 允许

## 故障排查

### 问题 1: 无法安装
- 检查是否允许"安装未知应用"
- 检查手机存储空间是否充足

### 问题 2: 宠物不显示
- 检查悬浮窗权限是否授予
- 检查应用是否在后台运行
- 尝试重启应用

### 问题 3: 服务被杀掉
- 在系统设置中将应用加入白名单
- 关闭省电模式对该应用的限制
- 锁定应用在最近任务列表中

## 发布版本（Release Build）

### 签名配置
```bash
# 生成签名密钥
keytool -genkey -v -keystore desktop-pet.keystore -alias desktop-pet -keyalg RSA -keysize 2048 -validity 10000

# 签名 APK
.\gradlew.bat assembleRelease
```

### 优化选项
- 启用代码混淆：`isMinifyEnabled = true`
- 启用资源压缩：`isShrinkResources = true`
- 使用 R8 编译器优化

## 项目自定义

### 修改宠物外观
编辑 `PetDrawView.kt` 文件：
- 修改 `bodyColor` 改变身体颜色
- 修改 `drawBody()` 方法改变形状
- 添加新的绘制方法实现自定义角色

### 添加新动作
编辑 `PetState.kt` 文件：
- 在 `Action` 枚举中添加新动作
- 在 `startIdleBehavior()` 中实现动作逻辑

### 添加新表情
编辑 `PetDrawView.kt` 文件：
- 在 `Expression` 枚举中添加新表情
- 在 `drawEyes()` 和 `drawMouth()` 中实现绘制

## 技术支持

- 官方文档：项目根目录 README.md
- 问题反馈：项目 Issues 页面
- 开发交流：参考 Android 官方文档

---

**祝你使用愉快！享受你的桌面小宠物 🐱**

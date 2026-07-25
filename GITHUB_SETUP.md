# GitHub 快速部署指南

## 🚀 一键推送到 GitHub

### 方法 1：使用脚本（推荐）

运行以下命令将项目推送到 GitHub：

```powershell
# 1. 进入项目目录
cd DesktopPet

# 2. 初始化 Git（如果还没有）
git init

# 3. 添加所有文件
git add .

# 4. 提交
git commit -m "✨ 桌面互动小宠物 - 初始版本"

# 5. 添加远程仓库（替换为你的 GitHub 用户名）
git remote add origin https://github.com/YOUR_USERNAME/DesktopPet.git

# 6. 推送到 GitHub
git push -u origin main
```

### 方法 2：使用 GitHub CLI

```powershell
# 安装 GitHub CLI（如果还没有）
winget install GitHub.cli

# 登录
gh auth login

# 创建仓库并推送
gh repo create DesktopPet --public --push --source .
```

## 📋 详细步骤

### 第一步：创建 GitHub 仓库

1. 打开 https://github.com
2. 点击右上角 "+" → "New repository"
3. 填写：
   - **Repository name**: `DesktopPet`
   - **Description**: `一个可爱的安卓桌面互动小宠物应用`
   - **Public**: ✅（公开）
   - **不要勾选** Initialize this repository with a README
4. 点击 "Create repository"

### 第二步：本地初始化

```powershell
cd DesktopPet
git init
git add .
git commit -m "✨ 桌面互动小宠物 v1.0"
```

### 第三步：推送代码

```powershell
# 添加远程仓库（替换 YOUR_USERNAME）
git remote add origin https://github.com/YOUR_USERNAME/DesktopPet.git

# 推送（可能需要输入 GitHub 用户名和密码）
git push -u origin main
```

**注意**：2021年后 GitHub 需要使用 Personal Access Token 而不是密码：
1. GitHub → Settings → Developer settings → Personal access tokens
2. Generate new token (classic)
3. 勾选 `repo` 权限
4. 复制生成的 token 作为密码使用

### 第四步：验证构建

1. 打开 https://github.com/YOUR_USERNAME/DesktopPet
2. 点击 "Actions" 标签
3. 应该看到 "Build Android APK" 工作流正在运行
4. 等待 5-10 分钟
5. 点击工作流 → Artifacts → 下载 APK

## 🔧 添加 Release 签名（可选）

Release APK 需要签名密钥：

### 1. 生成签名密钥

```powershell
# 生成签名密钥库
keytool -genkey -v -keystore desktop-pet.keystore -alias desktop-pet -keyalg RSA -keysize 2048 -validity 10000

# 导出为 Base64（用于 GitHub Secrets）
[Convert]::ToBase64String([IO.File]::ReadAllBytes("desktop-pet.keystore"))
```

### 2. 添加 GitHub Secrets

1. 打开 GitHub 仓库 → Settings → Secrets and variables → Actions
2. 添加以下 Secrets：

| Secret Name | Value |
|-------------|-------|
| `KEYSTORE_BASE64` | 密钥库的 Base64 编码 |
| `KEYSTORE_PASSWORD` | 密钥库密码 |
| `KEY_ALIAS` | `desktop-pet` |
| `KEY_PASSWORD` | 密钥密码 |

### 3. 修改构建配置

项目已包含签名配置，无需额外修改。

## 🎯 完整工作流程

```
代码变更 → Push → GitHub Actions → 自动构建 → APK 生成 → Releases 发布
```

### 每次更新流程

```powershell
# 1. 修改代码
# ... 编辑文件 ...

# 2. 提交并推送
git add .
git commit -m "✨ 新功能：xxx"
git push

# 3. GitHub Actions 自动构建
# 查看: https://github.com/YOUR_USERNAME/DesktopPet/actions

# 4. 下载 APK
# Actions → Build Debug APK → Artifacts → app-debug-apk
```

## 📱 下载安装 APK

### 从 Actions 下载
1. GitHub 仓库 → Actions 标签
2. 选择最新的工作流运行
3. 点击 "app-debug-apk" 下载

### 从 Releases 下载（Release 版本）
1. GitHub 仓库 → Releases 标签
2. 点击最新的 release
3. 下载 app-release.apk

### 安装到手机
1. 传输 APK 到手机
2. 允许"安装未知应用"
3. 安装并运行
4. 授予悬浮窗权限

## ❓ 常见问题

### Q: 构建失败怎么办？
A: 查看 Actions 日志，常见问题：
- 依赖下载失败 → 重试
- 代码错误 → 检查错误信息并修复

### Q: 如何更新代码？
A:
```powershell
git add .
git commit -m "更新内容"
git push
```

### Q: 如何删除仓库？
A: GitHub 仓库 → Settings → Danger Zone → Delete this repository

### Q: 推送需要密码？
A: 使用 Personal Access Token 代替密码

## 📞 获取帮助

- GitHub Actions 文档: https://docs.github.com/actions
- GitHub Issues: https://github.com/YOUR_USERNAME/DesktopPet/issues

---

**提示**：保持仓库 Public 可以让更多人看到你的项目！

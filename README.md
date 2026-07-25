# Android 桌面互动小宠物 🐾

一个安卓桌面悬浮窗宠物应用，使用**真人照片**作为宠物外观，支持触摸互动、行走动画、巡边漫游和唤起豆包APP。

> ⚠️ 本 App 不商用，仅供娱乐。宠物照片为 AI 生成。

## 预览

![桌面宠物](https://webcdn.m.qq.com/webcdn/qclaw/expert/icons/1777034713775_.png)

## 功能特性

| 功能 | 说明 |
|------|------|
| 🖼️ **真人照片宠物** | 8 种表情（开心、伤心、害羞、惊讶、困倦等）+ 8 张行走关键帧 |
| 👆 **短按互动** | 12 种随机气泡文字反应，提升好感度 |
| 👆👆 **双击** | 超开心，爱心眼表情 |
| ✨ **双击表情** | 巡边时触摸立即「回家」 |
| ✊ **长按 0.5 秒** | 打开手机上的 **豆包 APP** |
| 🚶 **巡边漫游** | **10 秒无操作** → 自动走到左边框 → 沿边框顺时针行走（旋转适配） |
| 🎈 **气泡文字** | 互动时头顶弹出泡泡对话框，3 秒自动消失 |
| 🛞 **行走动画** | 7 帧行走循环（250ms/帧）+ 弹跳与倾斜效果 |
| 😊 **心情系统** | 开心 / 生气 / 无聊 / 困倦 / 兴奋 |
| 🧭 **自动卖萌** | 待机时随机说话、走动、嘟嘴 |

## 交互演示

```
待机 ──→ 10秒不理 ──→ 向左走到手机边框
                                │
                                ▼
                    ┌─ 左边框（向上走，身体横向）
                    │  顺时针 │
                  上边框（从左到右，倒立） ←──┐
                    │                        │
                  右边框（向下走，身体横向    │
                    │                        │
                  底边框（从右到左，正常）──
                                │
                    你触摸一下 │
                                ▼
                    走回屏幕中下方靠右 ──→ 待机
```

## 技术栈

- **Kotlin** - 主体语言
- **Service + WindowManager** - 桌面悬浮窗
- **ImageView + FrameLayout** - 图片渲染与气泡布局
- **ValueAnimator** - 弹跳动效与行走帧同步
- **Handler + Coroutine** - 巡边位置循环与空闲检测
- **Gradle + GitHub Actions** - 自动构建 APK

## 项目结构

```
DesktopPet/
├── app/src/main/java/com/example/desktoppet/
│   ├── MainActivity.kt        # 启动/停止界面（Jetpack Compose）
│   ├── PetService.kt          # 悬浮窗服务 + 触摸监听 + 巡边位置更新
│   ├── PetState.kt            # 状态管理（表情/动作/巡边状态机/10秒空闲检测）
│   └── PetImageView.kt        # 图片显示 + 气泡文字 + 动画帧 + 边缘旋转
├── app/src/main/res/
│   ├── drawable/
│   │   ├── speech_bubble_bg.xml   # 半透明气泡背景
│   │   ├── ic_pet_notification.xml# 通知图标
│   │   └── ic_stop.xml            # 停止按钮
│   └── drawable-nodpi/
│       ├── pet_happy.png      # 表情：开心
│       ├── pet_sad.png        # 表情：伤心
│       ├── pet_idle.png       # 表情：发呆
│       ├── pet_excited.png    # 表情：兴奋
│       ├── pet_shy.png        # 表情：害羞
│       ├── pet_surprise.png   # 表情：惊讶
│       ├── pet_sleep.png      # 表情：困倦
│       ├── pet_kiss.png       # 表情：亲亲
│       └── pet_walk_1..7.png  # 行走关键帧（透明PNG）
├── gradle/wrapper/
├── build.gradle.kts
├── settings.gradle.kts
└── .github/workflows/build.yml  # GitHub Actions 自动构建
```

## 安装方式

### 方式一：下载构建产物（推荐）

1. 进入 [GitHub Actions](https://github.com/songmumu/DesktopPet/actions) 页面
2. 选择最新的成功构建
3. 下载 `Artifacts` → `app-debug.apk` 或 `app-release-unsigned.apk`
4. 传输到安卓手机安装

### 方式二：Fork 后自动构建

1. Fork 本项目到你自己的 GitHub
2. 进入 Actions 标签页，启用 GitHub Actions
3. 手动触发构建或提交代码自动构建
4. 从 Artifacts 下载 APK

### 方式三：本地构建

```bash
# 需要 Android Studio + Android SDK
git clone https://github.com/songmumu/DesktopPet.git
cd DesktopPet
# 用 Android Studio 打开项目
# Run > Run 'app'
```

## 权限说明

| 权限 | 用途 | 必要性 |
|------|------|--------|
| `SYSTEM_ALERT_WINDOW` | 显示桌面悬浮窗 | ✅ 必需 |
| `FOREGROUND_SERVICE` | 后台保活 | ✅ 必需 |
| `POST_NOTIFICATIONS` | 通知栏运行提示 | ⚠️ Android 13+ 需授予 |

## 使用说明

1. **安装 APK** → **打开 App**
2. 点击 **「开启宠物」** → 授权悬浮窗权限
3. 宠物出现在桌面上，开始互动！

### 互动方式

| 操作 | 效果 |
|------|------|
| 短按宠物 | 随机表情 + 气泡文字 |
| 双击宠物 | 爱心眼 + 超开心 |
| 长按 0.5 秒 | 打开豆包 APP |
| 拖拽 | 移动宠物位置 |
| 10 秒不互动 | 自动巡边行走 |
| 巡边时触摸 | 回家 → 待机 |

## 开发环境

- Android Studio Ladybug / Koala 或更高版本
- Kotlin 1.9+
- Compile SDK 35
- Min SDK 24 (Android 7.0)
- Gradle 8.x

## 常见问题

**Q: 为什么巡边时宠物会旋转？**
A: 为了「脚踩边框」，宠物会根据所在边框方向旋转：左→横向、上→倒立、右→横向、底→直立。

**Q: 长按没有打开豆包？**
A: 确保手机已安装豆包 APP（包名 `com.larus.nova`），可在设置 → 应用中查看。

**Q: 宠物不动了？**
A: 检查是否被系统杀掉后台。可以在系统设置中将本 App 加入「受保护应用」或「自启动」列表。

**Q: 如何更换宠物照片？**
A: 替换 `drawable-nodpi/` 下的 PNG 图片（保持同名），重新构建即可。

## License

MIT License - 自由使用和修改

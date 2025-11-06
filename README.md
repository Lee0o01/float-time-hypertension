# Float Time Hypertension - 悬浮计时器

一个轻量级的Android悬浮窗计时器应用，支持正计时和倒计时功能。

## 功能特点

- 🕐 **正计时/倒计时**：支持两种计时模式切换
- 📱 **悬浮窗显示**：计时器悬浮在屏幕上方，不影响其他应用使用
- 🎯 **随意拖动**：可将悬浮窗拖拽到屏幕任意位置
- 🎨 **简洁界面**：直观的操作界面，易于使用
- 🔧 **后台运行**：使用前台服务确保计时器稳定运行

## 应用截图

![应用截图](screenshots/app_screenshot.png)

## 安装说明

### 系统要求
- Android 5.0 (API 21) 或更高版本

### 安装步骤
1. 下载最新发布的APK文件
2. 在Android设备上打开APK文件进行安装
3. 首次运行时需要授予悬浮窗权限

## 使用指南

1. **启动应用**：打开应用后会自动请求悬浮窗权限
2. **权限设置**：首次使用需要在系统设置中允许"显示在其他应用上方"
3. **操作说明**：
   - 点击 ▶️ 按钮开始/暂停计时
   - 点击 🔄 按钮重置计时器
   - 点击 🔁 按钮切换正计时/倒计时模式
   - 点击 ❌ 按钮最小化/展开控制面板
   - 长按悬浮窗可拖动到任意位置

## 技术实现

### 核心组件
- `MainActivity`：主活动，负责权限申请和启动服务
- `FloatingTimerService`：核心服务，实现悬浮窗和计时功能
- `overlay_floating.xml`：悬浮窗界面布局

### 关键技术
- WindowManager：实现悬浮窗显示
- Foreground Service：确保后台持续运行
- Handler/Runnable：实现正计时逻辑
- CountDownTimer：实现倒计时功能

## 构建说明

### 本地构建
```bash
# 克隆项目
git clone <repository-url>

# 进入项目目录
cd float-time-hypertension

# 构建Debug版本
./gradlew assembleDebug

# 构建Release版本
./gradlew assembleRelease
```

### GitHub Actions 自动构建
项目已配置GitHub Actions，每次推送到main分支会自动触发构建流程。

## 权限说明

- `SYSTEM_ALERT_WINDOW`：用于显示悬浮窗
- `FOREGROUND_SERVICE`：用于在前台运行服务

## 常见问题

### 1. 悬浮窗不显示
请检查是否已授予悬浮窗权限：
设置 → 应用管理 → 悬浮计时器 → 权限 → 显示在其他应用上方

### 2. 计时器在后台被杀死
部分手机厂商的省电策略可能会杀死后台服务，请将应用加入白名单。

## 开源许可

本项目采用MIT许可证，详情请查看 [LICENSE](LICENSE) 文件。

## 贡献

欢迎提交Issue和Pull Request来改进本项目。
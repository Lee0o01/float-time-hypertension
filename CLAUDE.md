# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 常用命令

- **构建 Debug 版本**:
  ```bash
  ./gradlew assembleDebug
  ```
- **构建 Release 版本**:
  ```bash
  ./gradlew assembleRelease
  ```
- **清理构建产物**:
  ```bash
  ./gradlew clean
  ```
- **运行单元测试**:
  ```bash
  ./gradlew testDebugUnitTest
  ```
- **运行连接设备上的 Android 测试**:
  ```bash
  ./gradlew connectedDebugAndroidTest
  ```

## 高层级代码架构和结构

该项目是一个轻量级的 Android 悬浮窗计时器应用，主要功能是提供正计时和倒计时，并在屏幕上方以悬浮窗形式显示，支持拖动和后台运行。

- **核心功能**:
  - 支持正计时和倒计时两种模式。
  - 计时器以悬浮窗形式显示，不干扰其他应用。
  - 悬浮窗可自由拖动到屏幕任意位置。
  - 通过前台服务保证计时器在后台稳定运行。

- **核心组件**:
  - `MainActivity` (主活动): 位于 `app/src/main/java/com/example/myapp/MainActivity.java`。
    - 负责处理应用启动时的权限申请（特别是悬浮窗权限）
    - 使用 `ActivityResultLauncher` 现代化权限处理方式（`MainActivity.java:26-29`）
    - 检查并请求 `SYSTEM_ALERT_WINDOW` 权限后启动 `FloatingTimerService`
    - 应用启动后会自动将任务移至后台（`MainActivity.java:59`）

  - `FloatingTimerService` (核心服务): 位于 `app/src/main/java/com/example/myapp/FloatingTimerService.java`。
    - 继承自 `Service`，实现所有计时和悬浮窗显示逻辑
    - 通过 `WindowManager` 和自定义布局 `overlay_floating.xml` 创建悬浮窗
    - 正计时使用 `Handler/Runnable` 每秒更新一次（`FloatingTimerService.java:163-174`）
    - 倒计时使用 `CountDownTimer`，默认倒计时时长为5分钟（`FloatingTimerService.java:42`）
    - 支持触摸拖拽移动位置（`FloatingTimerService.java:141-161`）
    - 支持展开/最小化两种显示模式（`FloatingTimerService.java:36-37`）
    - 创建前台服务通知确保服务不被系统杀死（`FloatingTimerService.java:64-74`）
    - 计时结束时发送高优先级通知提醒用户（`FloatingTimerService.java:238-248`）

  - `overlay_floating.xml` (布局文件): 位于 `app/src/main/res/layout/overlay_floating.xml`。
    - 定义悬浮窗界面，包含计时显示区和控制按钮区
    - 控制按钮包括：开始/暂停、重置、模式切换、最小化

- **关键技术**:
  - `WindowManager`: Android 系统服务，用于管理和显示悬浮窗（`FloatingTimerService.java:94`）。使用 `WindowManager.LayoutParams` 控制悬浮窗位置和属性（`FloatingTimerService.java:97-108`）。

  - `Foreground Service`: 确保 `FloatingTimerService` 在后台运行时不会被系统轻易杀死。在 Android 8.0+ 上使用 `TYPE_APPLICATION_OVERLAY` 窗口类型（`FloatingTimerService.java:100-102`），在早期版本使用 `TYPE_PHONE`。

  - `Handler/Runnable`: 主要用于实现正计时逻辑，每秒更新一次计时器（`FloatingTimerService.java:164-173`）。通过 `handler.postDelayed()` 实现周期性执行。

  - `CountDownTimer`: Android 提供的工具类，用于实现倒计时功能（`FloatingTimerService.java:210-229`）。倒计时结束时自动停止并发送通知。

  - `MotionEvent`: 处理悬浮窗的触摸和拖拽交互（`FloatingTimerService.java:141-161`）。监听 `ACTION_DOWN` 记录起始位置，`ACTION_MOVE` 计算拖拽距离。

  - `NotificationChannel` 和 `NotificationCompat`: 创建前台服务通知（`FloatingTimerService.java:64-91`）和计时结束提醒（`FloatingTimerService.java:238-248`）。使用低优先级通知作为前台服务提示，计时结束时使用高优先级通知。

- **构建系统**:
  - 项目使用 **Gradle** 作为构建工具。
  - 根目录下的 `build.gradle` 定义了项目的全局构建配置，包括依赖仓库和 Gradle 插件版本。
  - `app/build.gradle` 包含了应用的具体配置，如 `compileSdkVersion` (编译SDK版本 30)、`minSdkVersion` (最低SDK版本 21)、`targetSdkVersion` (目标SDK版本 30)、`applicationId` ("com.example.myapp") 以及应用的各种库依赖，例如 `androidx.appcompat`、`androidx.constraintlayout` 和 `com.google.android.material` 等。

- **权限说明**:
  - `SYSTEM_ALERT_WINDOW`: 必需的权限，允许应用绘制在其他应用上方，从而实现悬浮窗功能。
  - `FOREGROUND_SERVICE`: 必需的权限，用于声明应用会运行一个前台服务，以在后台长时间执行任务而不被系统终止。

## GitHub Actions 自动化（待推送后启用）
项目已配置 **GitHub Actions** (`.github/workflows/android.yml`)，待项目推送到 GitHub 后即可使用：

- 每次推送到 `main` 或 `develop` 分支时自动触发构建
- CI/CD 工作流使用 JDK 17 和 Gradle 7.6.1
- 自动构建 **Release 和 Debug 版本 APK**
- 构建完成后作为 GitHub Artifacts 上传（保留30天）
- 支持从 Actions 页面直接下载 APK 文件

## 新手开发指南

### 环境准备

#### 1. 安装 Android Studio
**Windows 系统**：
1. 访问 https://developer.android.com/studio
2. 点击 "Download Android Studio"
3. 下载完成后双击安装包（`.exe` 文件）
4. 安装向导步骤：
   - 选择安装类型：推荐 "Standard"
   - 选择 UI 主题：个人喜好（暗色/亮色）
   - 确认安装位置：默认 `C:\Program Files\Android\Android Studio`
   - 点击 "Next" → "Install"
   - 安装完成后点击 "Finish"

**macOS 系统**：
1. 下载 `.dmg` 文件
2. 双击挂载镜像文件
3. 将 Android Studio 拖拽到 Applications 文件夹
4. 启动台中找到 Android Studio 并启动

**首次启动配置**：
- 启动后会提示 "Complete Installation"
- 选择 "Standard" 设置类型
- 确认 SDK 路径（通常自动检测）
- 点击 "Next" → "Finish"
- 等待 SDK 组件下载（约 1-5 分钟，取决于网络）

#### 2. 配置环境变量（推荐）
**Windows**：
```bash
# 打开系统属性 → 高级 → 环境变量
# 新建系统变量：
ANDROID_HOME=C:\Users\%USERNAME%\AppData\Local\Android\Sdk

# 编辑系统变量 Path，添加：
%ANDROID_HOME%\tools
%ANDROID_HOME%\platform-tools
%ANDROID_HOME%\emulator
```

**macOS/Linux**：
```bash
# 编辑 ~/.bashrc 或 ~/.zshrc
export ANDROID_HOME=$HOME/Android/Sdk
export PATH=$PATH:$ANDROID_HOME/tools:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator
source ~/.bashrc  # 或 source ~/.zshrc
```

**验证配置**：
```bash
# 打开命令行/终端，输入：
echo $ANDROID_HOME  # macOS/Linux
echo %ANDROID_HOME% # Windows

adb version  # 查看 ADB 工具版本
emulator -version  # 查看模拟器版本
```

#### 3. 连接设备或启动模拟器

**使用物理设备**：
1. 用 USB 线连接手机到电脑
2. 手机端操作：
   - 打开 "设置" → "关于手机"
   - 连续点击 "版本号" 7 次开启开发者选项
   - 返回 "设置" → "系统" → "开发者选项"（或"更多设置"→"开发者选项"）
   - 开启 "USB 调试"
   - 弹出 "允许 USB 调试" 对话框，勾选 "一律允许使用这台计算机进行调试" → "确定"

3. 电脑端验证：
   ```bash
   adb devices
   # 应该显示类似：List of devices attached
   #                   1234567890ABCDEF    device
   ```

**使用模拟器**：
1. 打开 Android Studio
2. 点击工具栏 "AVD Manager" 图标（或 "Tools" → "AVD Manager"）
3. 点击 "Create Virtual Device"
4. 选择设备型号：
   - 推荐："Pixel 6" 或 "Pixel 5"（较新，兼容性更好）
   - 或选择 "Phone" → "Nexus 5X"（经典款）
5. 选择系统镜像：
   - 推荐 API 30（Android 11，项目 targetSdkVersion）
   - 如果未下载，点击 "Download" 下载
6. 配置 AVD：
   - 名称：保持默认或自定义
   - 启动方向：Portrait（竖屏）
   - 相机：可以保持默认
7. 点击 "Finish" 创建
8. 返回 AVD Manager，点击绿色 "▶️" 按钮启动模拟器
9. 等待模拟器启动完成（约 2-5 分钟）

**验证模拟器**：
```bash
adb devices
# 应该显示：emulator-5554   device
```

### 本地构建步骤

#### 1. 克隆项目
```bash
# 如果是从 GitHub 克隆
git clone <repository-url>
cd float-time-hypertension

# 如果是本地现有项目，直接进入目录
cd float-time-hypertension
```

#### 2. 使用 Android Studio 打开项目
1. 启动 Android Studio
2. 点击 "Open an Existing Project"（不要选择 "New Project"）
3. 导航到项目根目录 `float-time-hypertension`
4. 点击 "OK" 打开
5. **首次打开需等待 Gradle 同步**：
   - 进度条显示："Building 'float-time-hypertension' project"
   - 可能提示下载 Gradle 7.6.1，等待下载完成
   - 同步完成后底部显示："Gradle sync finished"

**Gradle 同步失败时的解决方法**：
```bash
# 方法1：清理并重新构建
./gradlew clean
./gradlew build --refresh-dependencies

# 方法2：删除 .gradle 缓存目录（谨慎操作）
rm -rf .gradle  # macOS/Linux
rmdir /s .gradle  # Windows
```

#### 3. 构建 Debug 版本（开发测试）
**通过 Android Studio（推荐新手）**：
1. 确保设备/模拟器已连接且可见（顶部工具栏显示设备名称）
2. 点击工具栏绿色 "▶️" 运行按钮（或按 `Shift+F10`）
3. Android Studio 自动：
   - 执行 `./gradlew assembleDebug`
   - 安装 APK 到设备
   - 启动应用

**通过命令行**：
```bash
# 确保在项目根目录
cd float-time-hypertension

# 给 gradlew 执行权限（Linux/macOS 首次必须）
chmod +x gradlew

# 构建 Debug APK
./gradlew assembleDebug

# 检查构建结果
ls app/build/outputs/apk/debug/
# 应该显示：app-debug.apk

# 安装到设备/模拟器
./gradlew installDebug

# 验证安装
adb shell pm list packages | grep myapp
# 应该显示：package:com.example.myapp
```

**构建失败常见错误**：
- **"SDK not found"**：设置 SDK 路径
  - Android Studio: File → Project Structure → SDK Location
- **"Build failed"**：查看错误详情
  - 查看 "Build" 面板底部错误信息
  - 常见原因：网络问题（依赖下载失败）

#### 4. 构建 Release 版本（发布用）
```bash
# 构建 Release APK（未签名）
./gradlew assembleRelease

# 检查构建结果
ls app/build/outputs/apk/release/
# 应该显示：app-release.apk
```

**Release 版本特点**：
- 优化了代码和资源，文件更小
- 移除了调试信息
- 未签名（需手动签名才能发布到应用商店）

**签名 APK（如需发布）**：
```bash
# 生成签名密钥（仅需执行一次）
keytool -genkey -v -keystore release-key.keystore -alias alias_name -keyalg RSA -keysize 2048 -validity 10000

# 在 app/build.gradle 中添加签名配置（见下文）
# 重新构建已签名的 Release APK
./gradlew assembleRelease
```

**app/build.gradle 签名配置示例**：
```gradle
android {
    ...
    signingConfigs {
        release {
            storeFile file('release-key.keystore')
            storePassword 'your_store_password'
            keyAlias 'alias_name'
            keyPassword 'your_key_password'
        }
    }
    buildTypes {
        release {
            signingConfig signingConfigs.release
            minifyEnabled false
            ...
        }
    }
}
```

#### 5. 常用 Gradle 命令
```bash
# 清理所有构建产物
./gradlew clean

# 重新构建项目
./gradlew build

# 构建所有变体
./gradlew assemble

# 运行单元测试
./gradlew testDebugUnitTest

# 查看依赖树
./gradlew app:dependencies

# 列出所有任务
./gradlew tasks

# 带详细信息构建（调试用）
./gradlew assembleDebug --info

# 并行构建（加速）
./gradlew assembleDebug --parallel
```

### Android Studio 界面与项目结构

#### 界面布局
打开项目后，Android Studio 默认布局：
- **左侧**：项目文件树（Project 面板）
  - 点击 "Android" 视图查看按功能分组的文件
  - 点击 "Project" 视图查看完整文件结构
- **中间**：代码编辑器
- **右侧**：布局预览（对于 XML 文件）
- **底部**：工具面板
  - "Logcat"：查看应用日志
  - "Run"：查看构建和运行日志
  - "Terminal"：命令行终端
  - "Build"：构建进度和结果

#### 项目结构说明
```
float-time-hypertension/
├── app/                    # 应用模块
│   ├── build/             # 构建产物（自动生成）
│   ├── src/
│   │   └── main/
│   │       ├── java/com/example/myapp/  # Java 源码
│   │       │   ├── MainActivity.java    # 主活动
│   │       │   └── FloatingTimerService.java  # 悬浮窗服务
│   │       ├── res/                      # 资源文件
│   │       │   ├── drawable/             # 图片资源
│   │       │   ├── layout/               # 布局文件
│   │       │   ├── mipmap/               # 应用图标
│   │       │   ├── values/               # 字符串/颜色等
│   │       │   └── AndroidManifest.xml   # 清单文件
│   ├── build.gradle       # 应用级构建配置
│   └── proguard-rules.pro # 代码混淆规则
├── gradle/                # Gradle 包装器
├── .github/               # GitHub 配置（CI/CD）
├── build.gradle           # 项目级构建配置
├── settings.gradle        # 项目设置
├── gradlew                # Gradle 包装器脚本（Linux/macOS）
├── gradlew.bat            # Gradle 包装器脚本（Windows）
└── CLAUDE.md              # 项目说明文档
```

#### 常用快捷键
**Windows/Linux**：
- `Ctrl + Shift + O`：搜索文件
- `Ctrl + Shift + F`：搜索代码
- `Ctrl + D`：复制当前行
- `Ctrl + Y`：删除当前行
- `Ctrl + /`：注释/取消注释
- `Shift + F10`：运行应用
- `Ctrl + F9`：重新构建
- `Ctrl + Shift + A`：查找操作

**macOS**：
- `Cmd + Shift + O`：搜索文件
- `Cmd + Shift + F`：搜索代码
- `Cmd + D`：复制当前行
- `Cmd + Delete`：删除当前行
- `Cmd + /`：注释/取消注释
- `Shift + Cmd + R`：运行应用
- `Cmd + F9`：重新构建
- `Shift + Cmd + A`：查找操作

### 应用运行与调试

#### 1. 运行应用
**方法1：直接运行（推荐新手）**
1. 在 Android Studio 中打开项目
2. 确保设备/模拟器已连接（顶部工具栏显示设备名）
3. 点击绿色 "▶️" 运行按钮
4. 或按 `Shift + F10`（Windows/Linux）/ `Shift + Cmd + R`（macOS）
5. 等待构建和安装完成，应用自动启动

**方法2：命令行运行**
```bash
# 确保设备连接
adb devices

# 安装并运行
./gradlew installDebug

# 启动应用（如果已安装）
adb shell am start -n com.example.myapp/.MainActivity
```

#### 2. 查看日志（Logcat）
**通过 Android Studio**：
1. 点击底部 "Logcat" 标签
2. 设置过滤器：
   - Package Name：`com.example.myapp`
   - Log Level：`Info` 或 `Debug`
3. 查看实时日志输出

**通过命令行**：
```bash
# 查看所有日志
adb logcat

# 只看应用日志（推荐）
adb logcat | grep "FloatingTimer"

# 查看指定级别日志
adb logcat *:I  # Info 及以上
adb logcat *:D  # Debug 及以上

# 保存日志到文件
adb logcat > myapp_logs.txt

# 清除日志缓存
adb logcat -c
```

**重要日志示例**：
- 应用启动：`I/com.example.myapp: onCreate()`
- 服务启动：`I/FloatingTimerService: onCreate()`
- 悬浮窗创建：`I/FloatingTimerService: WindowManager initialized`
- 权限检查：`I/MainActivity: Overlay permission granted`

#### 3. 调试技巧
**设置断点**：
1. 在代码编辑器左侧行号区域点击（出现红色圆点）
2. 常见断点位置：
   - `MainActivity.java:36`（权限检查函数）
   - `FloatingTimerService.java:176`（开始计时函数）
   - `FloatingTimerService.java:142`（触摸处理函数）

**运行调试模式**：
1. 设置断点后，点击工具栏 "🐛" 按钮（或按 `Shift + F9`）
2. 应用在断点处暂停
3. 使用调试工具栏：
   - `▶️`：继续执行
   - `⏭️`：跳过当前行
   - `⤵️`：进入函数内部
   - `⤴️`：从函数返回

**实时调试（Attach Debugger）**：
1. 应用已运行时，点击 "Run" → "Attach Debugger to Android Process"
2. 选择应用进程（`com.example.myapp`）
3. 调试器附加成功后，设置断点即可

#### 4. 常见问题调试
**应用崩溃（Crash）**：
```bash
# 查看崩溃日志
adb logcat | grep "AndroidRuntime"
# 或
adb logcat | grep "FATAL EXCEPTION"
```

**悬浮窗权限问题**：
1. 应用启动后会跳转到权限设置页面
2. 如果手动关闭，需重新授权：
   ```bash
   # 打开悬浮窗权限设置
   adb shell am start -n com.android.settings/.OverlaySettings -d package:com.example.myapp
   ```
3. 确认"允许显示在其他应用上层"已开启

**服务未启动**：
```bash
# 查看运行中的服务
adb shell dumpsys activity services | grep myapp

# 手动启动服务
adb shell am startservice -n com.example.myapp/.FloatingTimerService

# 停止服务
adb shell am stopservice -n com.example.myapp/.FloatingTimerService
```

### 测试

#### 1. 单元测试（当前暂无，建议添加）
**说明**：项目当前无单元测试，但这是最佳实践

**添加 JUnit 测试示例**：
```java
// 在 app/src/test/java/ 下创建测试文件
package com.example.myapp;

import org.junit.Test;
import static org.junit.Assert.*;

public class TimerUtilsTest {
    @Test
    public void testFormatTime() {
        // 测试计时格式转换
        String result = TimerUtils.formatTime(65000); // 65秒
        assertEquals("01:05", result);
    }
}
```

**运行单元测试**：
```bash
./gradlew testDebugUnitTest
# 或
./gradlew test  # 所有测试
```

#### 2. 集成测试（UI 测试）
**添加 Espresso 测试示例**：
```java
// 在 app/src/androidTest/java/ 下创建
package com.example.myapp;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;

@RunWith(AndroidJUnit4.class)
public class MainActivityTest {
    @Rule
    public ActivityScenarioRule<MainActivity> activityRule =
        new ActivityScenarioRule<>(MainActivity.class);

    @Test
    public void testStartButton() {
        onView(withId(R.id.startFloatingTimer))
            .perform(click())
            .check(matches(isDisplayed()));
    }
}
```

**运行集成测试**：
```bash
# 确保设备/模拟器已连接
./gradlew connectedDebugAndroidTest

# 查看测试报告
open app/build/reports/androidTests/connected/index.html  # macOS
start app/build/reports/androidTests/connected/index.html  # Windows
```

#### 3. 手动测试（详细步骤）
**测试环境**：Android 设备（真机或模拟器）

**步骤1：应用安装与权限授予**
1. 运行应用：
   - Android Studio 点击 "▶️" 或命令行 `./gradlew installDebug`
2. 应用启动后显示主界面（悬浮计时器标题 + 启动按钮）
3. 点击"启动悬浮计时器"按钮
4. 跳转到系统权限页面：
   - **Android 6.0-10**：直接显示开关
   - **Android 11+**：显示应用列表，需找到本应用并开启
5. 开启"允许显示在其他应用上层"权限
6. 返回应用，自动进入后台，悬浮窗出现在屏幕上方

**验证要点**：
- ✅ 权限页面正确跳转
- ✅ 权限开启后悬浮窗出现
- ✅ 悬浮窗显示位置正确（顶部，距顶部 100dp）

**步骤2：测试正计时功能**
1. 悬浮窗默认进入正计时模式
2. 点击 ▶️ 按钮（开始/暂停按钮）
3. 验证：
   - ✅ 按钮图标变为 ⏸️（暂停）
   - ✅ 计时数字从 00:00 开始递增
   - ✅ 每秒更新一次（观察5秒以上）
4. 再次点击 ⏸️ 按钮
   - ✅ 按钮图标变回 ▶️
   - ✅ 计时停止在当前值
5. 点击 🔄 按钮（重置）
   - ✅ 计时归零 00:00
   - ✅ 状态保持暂停（按钮为 ▶️）

**测试用例**：
- 正计时1分钟：观察数字是否持续递增
- 正计时暂停：停止后数字保持不变
- 正计时重置：任意时刻重置都为 00:00

**步骤3：测试倒计时功能**
1. 点击 🔁 按钮（切换模式）
2. 验证：
   - ✅ 模式切换提示（按钮图标变化）
   - ✅ 计时显示从 05:00 开始（默认5分钟）
3. 点击 ▶️ 开始倒计时
   - ✅ 数字每秒递减：05:00 → 04:59 → 04:58...
   - ✅ 倒计时至 00:00
4. 倒计时结束时：
   - ✅ 自动停止（按钮变为 ▶️）
   - ✅ 计时显示 00:00
   - ✅ 收到通知（顶部下拉应显示"计时结束"通知）

**步骤4：测试悬浮窗拖拽功能**
1. 长按悬浮窗空白区域（不要按按钮）
2. 拖拽到屏幕不同位置：
   - ✅ 拖拽流畅无卡顿
   - ✅ 松手后悬浮窗停在目标位置
3. 测试边界：
   - 拖拽到屏幕最左侧
   - 拖拽到屏幕最右侧
   - 拖拽到屏幕顶部和底部
   - ✅ 悬浮窗不会超出屏幕范围

**步骤5：测试展开/最小化**
1. 默认状态：展开（显示所有按钮）
2. 点击 ❌ 按钮（最小化）
   - ✅ 控制按钮容器隐藏
   - ✅ 只显示计时数字
   - ✅ 悬浮窗尺寸变小
3. 再次点击 ❌ 按钮
   - ✅ 按钮重新显示
   - ✅ 悬浮窗恢复原尺寸

**步骤6：测试后台运行**
1. 启动悬浮计时器并开始计时
2. 按 Home 键返回桌面
   - ✅ 悬浮窗仍显示并持续计时
3. 打开其他应用（如浏览器、微信）
   - ✅ 悬浮窗覆盖在其他应用上方
   - ✅ 计时持续进行
4. 锁屏（按电源键）
   - ✅ 解锁后计时继续
   - ✅ 时间准确（无跳变）
5. 持续运行10分钟
   - ✅ 计时器稳定运行
   - ✅ 服务未被杀死
   - ✅ 通知栏显示"计时器运行中"通知

**步骤7：测试多模式切换**
1. 正计时运行中 → 切换模式
   - ✅ 暂停当前计时
   - ✅ 显示 05:00（倒计时默认值）
2. 倒计时运行中 → 切换模式
   - ✅ 暂停倒计时
   - ✅ 从暂停位置继续正计时

**步骤8：测试异常情况**
1. 旋转屏幕（模拟器测试）
   - ✅ 服务不重启
   - ✅ 悬浮窗位置保持
   - ✅ 计时继续进行
2. 应用进程被杀死（开发者选项中）
   - ✅ 重新打开应用需重新授权权限
   - ✅ 计时器不会自动恢复（合理设计）

#### 4. 性能测试
```bash
# 查看内存使用
adb shell dumpsys meminfo com.example.myapp

# 查看CPU使用
adb shell top -n 1 | grep myapp

# 测试长时间运行
# 连续运行1小时，检查是否有内存泄漏
```

#### 5. 兼容性测试
在不同 Android 版本测试：
- ✅ Android 5.0（API 21）：最低支持版本
- ✅ Android 6.0（API 23）：运行时权限
- ✅ Android 8.0（API 26）：通知渠道、后台服务限制
- ✅ Android 10（API 29）：分区存储
- ✅ Android 11（API 30）：权限单次授权
- ✅ Android 12+（API 31+）：新权限模型

### GitHub Actions 自动构建说明（项目推送后）
**注意**：此功能仅在项目推送到 GitHub 后可用

1. **推送代码到 GitHub**：
   ```bash
   git add .
   git commit -m "Initial commit"
   git branch -M main
   git remote add origin <your-github-repo-url>
   git push -u origin main
   ```

2. **自动构建触发**：
   - 每当推送到 `main` 或 `develop` 分支时，GitHub Actions 会自动运行
   - 构建流程包括：检出代码 → 配置 JDK 17 → 配置 Gradle 7.6.1 → 构建 APK

3. **下载构建产物**：
   - 进入 GitHub 仓库的 "Actions" 标签页
   - 选择最新的工作流执行
   - 在 "Artifacts" 部分可下载：
     - `app-release.apk`（发布版本）
     - `app-debug.apk`（调试版本）

### 常见开发问题与解决方案

#### 1. Gradle 相关问题
**问题1：Gradle sync failed**
```
Could not resolve com.android.tools.build:gradle:7.4.2
```
**解决方案**：
```bash
# 清理缓存重新同步
./gradlew clean
./gradlew build --refresh-dependencies

# 删除本地缓存（谨慎）
rm -rf ~/.gradle/caches  # macOS/Linux
rmdir /s %USERPROFILE%\.gradle\caches  # Windows

# 重启 Android Studio
```

**问题2：Gradle version conflict**
```
Gradle 7.5 is required. Current version is 7.4.1
```
**解决方案**：
- 检查 `gradle/wrapper/gradle-wrapper.properties`：
  ```
  distributionUrl=https\://services.gradle.org/distributions/gradle-7.6.1-bin.zip
  ```
- Android Studio: File → Sync Project with Gradle Files

**问题3：Gradle Build Daemon memory error**
```
Daemon was destroyed
OutOfMemoryError: Java heap space
```
**解决方案**：
在 `gradle.properties` 中添加：
```properties
org.gradle.jvmargs=-Xmx4096m -XX:MaxPermSize=512m
org.gradle.parallel=true
org.gradle.daemon=true
```

#### 2. SDK/构建工具问题
**问题1：SDK not found**
```
SDK location not found
```
**解决方案**：
- Android Studio: File → Project Structure → SDK Location
- 设置 Android SDK location：`C:\Users\%USERNAME%\AppData\Local\Android\Sdk`
- 或环境变量设置：`ANDROID_HOME`

**问题2：Build tools version not found**
```
Build tools revision 30.0.3 is too low for project
```
**解决方案**：
1. Android Studio: Tools → SDK Manager
2. "SDK Tools" 选项卡
3. 勾选 "Android SDK Build-Tools 30.0.3" 或更高版本
4. 点击 "Apply" 下载

#### 3. 权限相关问题
**问题1：悬浮窗权限被拒绝**
```
Settings.canDrawOverlays(this) returns false
```
**解决方案**：
```bash
# 手动打开悬浮窗权限设置
adb shell am start -n com.android.settings/.OverlaySettings -d package:com.example.myapp

# 检查权限状态
adb shell dumpsys package com.example.myapp | grep SYSTEM_ALERT_WINDOW
# 应该显示：android.permission.SYSTEM_ALERT_WINDOW: granted=true
```

**问题2：FOREGROUND_SERVICE 权限缺失**
```
Permission Denial: startForeground requires permission
```
**解决方案**：
- 检查 `AndroidManifest.xml`：
  ```xml
  <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
  ```

#### 4. 运行时问题
**问题1：悬浮窗不显示**
可能原因与解决：
- **权限未授予**：检查"显示在其他应用上层"是否开启
- **WindowManager 初始化失败**：查看 Logcat 错误
- **布局文件错误**：检查 `overlay_floating.xml` 是否正确

**问题2：悬浮窗被系统杀死**
```bash
# 检查电池优化白名单（不同厂商路径不同）
adb shell dumpsys deviceidle whitelist | grep myapp

# 关闭电池优化（开发者选项中）
# 设置 → 电池 → 电池优化 → 找到应用 → 选择"不优化"
```

**问题3：服务重启异常**
- **Android 8.0+ 后台服务限制**：
  - 必须使用 `startForegroundService()`
  - 服务启动后5秒内必须调用 `startForeground()`
  - 检查 `FloatingTimerService.java:53-57` 实现

#### 5. 构建问题
**问题1：APK 安装失败**
```
INSTALL_FAILED_INSUFFICIENT_STORAGE
```
**解决方案**：
```bash
# 清理设备存储
adb shell pm clear com.example.myapp
adb uninstall com.example.myapp

# 重新安装
./gradlew installDebug
```

**问题2：签名问题**
```
Failure [INSTALL_PARSE_FAILED_NO_CERTIFICATES]
```
**解决方案**：
```bash
# 卸载旧版本（签名不一致）
adb uninstall com.example.myapp

# 清理构建产物
./gradlew clean

# 重新构建
./gradlew assembleDebug
```

#### 6. 调试问题
**问题1：Logcat 无输出**
```bash
# 清除日志缓冲区
adb logcat -c

# 设置过滤器
adb logcat com.example.myapp:I *:S

# 或重启 adb
adb kill-server
adb start-server
```

**问题2：断点不触发**
- 确保是 **Debug** 模式构建（默认 Debug 构建）
- 确保已选择 "Debug" 变体（Build Variants 面板）
- 检查断点是否在可执行代码上（非注释、非空行）

**问题3：Attach Debugger 失败**
- 应用必须处于运行状态
- 应用必须是 Debug 版本
- 设备/模拟器需要支持调试

#### 7. 性能问题
**问题1：内存泄漏**
```bash
# 检查内存使用
adb shell dumpsys meminfo com.example.myapp

# 生成 heap dump（需root权限）
adb shell am dumpheap com.example.myapp /sdcard/dump.hprof
adb pull /sdcard/dump.hprof
# 使用 MAT 或 Android Studio Memory Profiler 分析
```

**问题2：CPU 占用过高**
- 检查计时器是否频繁更新（正常情况每秒1次）
- 检查是否有死循环或递归调用
- 使用 Traceview 分析性能

#### 8. 网络问题（如果后续添加网络功能）
**问题1：依赖下载失败**
```
Connection timed out
```
**解决方案**：
```bash
# 配置代理（如果在中国）
# 在 gradle.properties 中添加：
systemProp.http.proxyHost=127.0.0.1
systemProp.http.proxyPort=7890
systemProp.https.proxyHost=127.0.0.1
systemProp.https.proxyPort=7890
```

#### 9. 版本兼容问题
**问题1：高版本 Android 崩溃**
- 检查 `minSdkVersion 21`（Android 5.0）
- 测试不同 API 版本
- 避免使用高版本独有的 API（添加版本检查）

**问题2：低版本 Android 功能异常**
- Android 5.0-6.0：无运行时权限提示
- Android 8.0+：后台服务限制
- Android 10+：分区存储

#### 10. Git/版本控制问题
**问题1：.gradle 文件是否提交？**
```
# .gradle 不应提交到 Git
echo ".gradle/" >> .gitignore
```

**问题2：构建产物提交**
```
# build/ 目录不应提交
echo "build/" >> .gitignore
```

**推荐 .gitignore**：
```
# Built application files
*.apk
*.aab

# Files for the ART/Dalvik VM
*.dex

# Java class files
*.class

# Generated files
bin/
gen/
out/
build/
gradle/

# Local configuration file (sdk path, etc)
local.properties

# Proguard folder generated by Eclipse
proguard/

# Log Files
*.log
```

### 代码结构说明
```
app/src/main/
├── java/com/example/myapp/
│   ├── MainActivity.java           # 主活动，权限处理
│   └── FloatingTimerService.java   # 核心服务，悬浮窗逻辑
├── res/
│   ├── layout/
│   │   ├── activity_main.xml       # 主界面布局
│   │   └── overlay_floating.xml    # 悬浮窗布局
│   ├── values/
│   │   └── strings.xml             # 字符串资源
│   └── drawable/
│       └── floating_background.xml # 悬浮窗背景
└── AndroidManifest.xml             # 清单文件，权限和组件声明
```

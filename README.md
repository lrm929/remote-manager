# Remote Manager - Android 远程管理 App

一款面向 Android 手机和平板的统一远程管理工具，支持 **Windows 远程桌面（RDP）** 与 **Linux SSH/SFTP 管理**，并针对一加平板 Pro 13.2 寸等大屏设备做了响应式适配。

## 功能特性

- **服务器管理**：本地保存 Windows / Linux 服务器配置，支持分组、搜索、增删改查。
- **RDP 远程桌面**：一键唤起 Microsoft Remote Desktop、aFreeRDP 等外部 RDP 客户端连接 Windows 服务器。
- **SSH 终端**：内置 JSch SSH 客户端，支持密码与私钥认证，提供交互式终端与快捷按键。
- **SFTP 文件管理**：浏览 Linux 服务器文件系统，下载文件到本地 Downloads 目录。
- **本地加密存储**：服务器密码、私钥使用 Android Keystore + EncryptedSharedPreferences 加密保存。
- **响应式布局**：
  - 手机：单栏导航
  - 小平板：双栏（列表 + 详情）
  - 一加平板 Pro 13.2 寸等大屏：双栏/三栏，充分利用大屏空间

## 环境要求

- Android Studio Hedgehog (2023.1.1) 或更新版本
- JDK 17
- Android SDK 34
- Gradle 8.4

## 项目结构

```
android-remote-manager/
├── app/
│   ├── src/main/java/com/remotemanager/
│   │   ├── MainActivity.kt
│   │   ├── RemoteManagerApp.kt
│   │   ├── data/          # Room 数据库、Repository、加密
│   │   ├── di/            # Koin 依赖注入
│   │   ├── rdp/           # RDP 外部启动
│   │   ├── ssh/           # SSH/SFTP 连接
│   │   └── ui/            # Compose UI（屏幕、组件、ViewModel）
│   └── src/main/res/      # 资源文件
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

## 构建步骤

### 方式一：本地用 Android Studio 编译

1. 用 Android Studio 打开 `android-remote-manager` 文件夹。
2. 等待 Gradle 同步完成（首次可能较长，需要下载依赖）。
3. 连接设备或启动模拟器。
4. 点击 `Run`（Shift + F10）编译并安装。

> 如果 Gradle 同步失败，请检查 `app/build.gradle.kts` 中的 `compileSdk`、`targetSdk` 是否与本机 SDK 版本匹配，并确认已安装 Android SDK 34。

### 方式二：通过 GitHub Actions 自动编译 APK

项目已配置 `.github/workflows/android.yml`， push 到 GitHub 后会自动编译并上传 APK。

1. 在 GitHub 新建一个仓库，把本项目代码 push 上去：
   ```bash
   git init
   git add .
   git commit -m "init"
   git branch -M main
   git remote add origin https://github.com/你的用户名/remote-manager.git
   git push -u origin main
   ```
2. 进入 GitHub 仓库的 **Actions** 页面，等待工作流运行完成。
3. 在最新一次运行的 **Summary** 页面底部找到 Artifacts，下载 `remote-manager-debug-apk`。
4. 解压 ZIP，把 `app-debug.apk` 发送到你的平板安装即可。

## 使用说明

### 1. 添加服务器

点击右下角 `+` 按钮，填写：

- **名称**：用于显示的服务器别名
- **类型**：RDP（Windows）或 SSH（Linux）
- **主机地址**：IP 或域名
- **端口**：RDP 默认 3389，SSH 默认 22
- **用户名**
- **认证**：密码或私钥（二选一）
- **分组 / 备注**（可选）
- **RDP 选项**：分辨率、颜色深度、NLA（仅 RDP 类型显示）

### 2. 连接 Windows 远程桌面

1. 确保平板已安装 RDP 客户端：
   - [Microsoft Remote Desktop](https://play.google.com/store/apps/details?id=com.microsoft.rdc.androidx)
   - 或 aFreeRDP
2. 在服务器列表中点击 RDP 服务器，进入详情页。
3. 点击 **"连接远程桌面 (RDP)"**。
4. 系统会唤起外部 RDP 客户端并带入连接参数。

> 如果未安装 RDP 客户端，App 会提示你安装。

### 3. 连接 Linux SSH 终端

1. 点击 SSH 服务器进入详情页。
2. 点击 **"SSH 终端"**。
3. 连接成功后，在底部输入命令并发送。
4. 快捷栏提供 `Ctrl+C`、`Ctrl+D`、`Ctrl+Z`、`Tab`、`Clear` 等常用按键。

### 4. SFTP 文件管理

1. 在 SSH 服务器详情页点击 **"SFTP 文件"**。
2. 浏览远程文件系统，点击文件可查看信息并下载。
3. 下载的文件会保存到系统 `Downloads` 目录。

## 针对一加平板 Pro 13.2 寸的适配

- **横屏双栏/三栏**：在大屏横屏下，左侧显示服务器列表，右侧同时显示详情与终端，减少页面跳转。
- **外接键盘支持**：终端界面支持常见快捷键，配合平板键盘盖或蓝牙键盘使用体验更佳。
- **可调整字体**：后续版本计划增加终端字体大小设置，进一步提升 13.2 寸屏幕可读性。
- **分屏与小窗**：适配 Android 多窗口模式，可在分屏或悬浮窗中运行。

## 安全说明

- 所有密码与私钥均通过 Android Keystore 加密后存入 `EncryptedSharedPreferences`，不会以明文写入 Room 数据库。
- 数据库和加密文件默认不参与云备份（已配置 `data-extraction-rules.xml` 与 `full-backup-content` 排除）。
- SSH 首次连接时采用 `StrictHostKeyChecking=no` 策略以简化使用；若用于生产环境，建议改为严格校验主机密钥。

## 已知限制

- RDP 功能依赖外部 RDP 客户端，App 本身不实现 RDP 协议渲染。
- SFTP 上传功能尚未实现，当前版本仅支持下载。
- 终端颜色与高亮为简化实现，复杂 ANSI 转义序列可能显示不完整。

## 技术栈

- Kotlin
- Jetpack Compose + Material3
- MVVM + Repository 模式
- Room + KSP
- Koin 依赖注入
- JSch（SSH/SFTP）
- EncryptedSharedPreferences（安全存储）

## 许可证

MIT License

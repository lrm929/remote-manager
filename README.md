# Remote Manager

一款开源的 Android 远程管理工具：在一个 App 里集中管理你的 **Windows 远程桌面（RDP）** 和 **Linux 服务器（SSH / SFTP）**，专为大屏平板打造的桌面软件式体验。

[![Android CI](https://github.com/lrm929/remote-manager/actions/workflows/android.yml/badge.svg)](https://github.com/lrm929/remote-manager/actions/workflows/android.yml)

## 这是什么软件？

如果你有一台 Android 平板（比如一加平板 Pro / 一加平板 2 Pro），又经常需要维护服务器，Remote Manager 就是为你准备的：

- 它像 **SecureCRT / Xshell** 一样，在左侧提供一棵可折叠的「会话管理」分组树，所有服务器一目了然；
- 打开的 SSH 终端、SFTP 文件管理器、服务器详情都以 **标签页** 形式共存，切换标签 **不断线**；
- 顶部工具栏一键连接 SSH、唤起 RDP、打开 SFTP；
- 所有密码和私钥都经过 **Android Keystore 硬件级加密** 后才落盘，本地存储，不上传任何服务器。

它不是 VNC/RDP 协议客户端——连接 Windows 桌面时，它会帮你唤起系统里已安装的 Microsoft Remote Desktop 等专业客户端并自动带入连接参数；而 SSH 终端和 SFTP 文件管理则是 App 内置完整实现的。

> **近期更新**：UI 已升级为深色科技感主题；SSH 终端改为直接在终端内输入命令；RDP 连接改为在 App 内打开标签页，并预留了 FreeRDP 原生引擎接入点。

## 功能一览

- **会话管理面板**：服务器按自定义分组组织成树，支持按名称过滤；面板可一键折叠成窄条，横屏办公空间最大化
- **多会话标签页**：详情 / SSH 终端 / SFTP / RDP / 编辑各自独立标签，随开随关，切标签保持连接
- **SSH 终端**：内置 JSch 客户端，支持密码与私钥认证；终端内直接输入，支持方向键、Ctrl 组合键、Tab、Esc 等常用控制键
- **SFTP 文件管理**：浏览远程目录、查看文件信息、下载到本地
- **RDP 快捷唤起**：一键拉起 Microsoft Remote Desktop / aFreeRDP 连接 Windows，分辨率、色深、NLA 可配（完整内置渲染需后续集成 FreeRDP 原生库）
- **加密存储**：密码、私钥使用 EncryptedSharedPreferences + Android Keystore 加密保存，数据库与密钥文件不参与云备份
- **大屏响应式**：手机单栏、平板横屏双区（面板 + 标签工作区），适配分屏与小窗模式

## 下载安装

方式一（推荐）：从 GitHub Actions 下载最新构建

1. 打开仓库的 [Actions 页面](https://github.com/lrm929/remote-manager/actions)，点击最近一次成功的运行；
2. 在页面底部 **Artifacts** 下载 `remote-manager-debug-apk`；
3. 解压 ZIP 得到 `app-debug.apk`，发送到平板安装（首次需允许「安装未知来源应用」）。

方式二：自己编译，见下文「自行构建」。

## 快速上手

### 1. 添加第一台服务器

点左侧会话管理面板右上角的 `+`（或工具栏第一个图标），填写：

- **名称**：服务器别名，如「家用 NAS」
- **类型**：RDP（Windows）或 SSH（Linux）
- **主机 / 端口**：IP 或域名；RDP 默认 3389，SSH 默认 22
- **用户名 + 认证方式**：密码或私钥二选一
- **分组**（可选）：如「生产环境」「个人」，会显示为会话树中的文件夹

### 2. 连接 Linux（SSH 终端）

在会话树中点选服务器 → 点工具栏的 **终端图标**（或详情页里的「SSH 终端」）→ 新标签页中即出现终端。直接在终端底部的 `$` 提示符后输入命令回车即可，支持 Ctrl+C / Ctrl+D / Ctrl+Z / Tab 等快捷按钮。

### 3. 浏览 / 下载文件（SFTP）

选中 SSH 服务器 → 点工具栏的 **文件夹图标**。点文件可查看详情并下载，下载内容保存在应用私有目录（可用文件管理器在 `Android/data/com.remotemanager/files/Download` 中找到）。

### 4. 连接 Windows（RDP）

选中 RDP 服务器 → 点工具栏的 **显示器图标**，App 会在内部标签页打开 RDP 会话页，点击「连接远程桌面」后会尝试唤起已安装的 Microsoft Remote Desktop 或 aFreeRDP 并带入地址、账号等参数。

> 当前版本 RDP 画面仍由外部客户端渲染。要在 App 内直接渲染远程桌面，需要后续集成 FreeRDP 原生库（NDK + CMake）。代码中已预留 `FreeRdpBridge` 接入点。

### 5. 高效使用小技巧

- 左侧面板挡住终端时，点面板标题栏的 `‹` 收起成竖条，再点竖条展开
- 同时开多台服务器的终端，点标签即可切换，**连接不会断**
- 配合键盘盖 / 蓝牙键盘使用，终端体验接近桌面 CRT 软件

## 安全说明

- 密码与私钥经 Android Keystore 加密后存入 `EncryptedSharedPreferences`，明文不会写入数据库
- 数据库与加密文件已通过 `data-extraction-rules.xml` / `backup_rules.xml` 排除在云备份之外
- SSH 默认采用 `StrictHostKeyChecking=no` 方便首次连接；面向生产环境建议自行改为严格校验主机密钥
- 本 App 不含任何统计、广告与上报代码，数据只存在你的设备上

## 已知限制

- RDP 目前仍依赖外部客户端渲染画面；App 内仅提供连接入口和参数传递
- SFTP 暂不支持上传，仅下载
- 终端 ANSI 颜色为高亮简化实现，极复杂的转义序列可能显示不完整
- 关闭 SSH 标签不会立即断开底层会话（重开同一服务器可恢复现场），退出 App 才会真正断开

## 自行构建

环境：Android Studio Hedgehog+、JDK 17、Android SDK 34、Gradle 8.4。

```bash
git clone https://github.com/lrm929/remote-manager.git
cd remote-manager
gradle assembleDebug    # 产物在 app/build/outputs/apk/debug/
```

也可以 Fork 本仓库后直接 push——CI（`.github/workflows/android.yml`）会自动构建并把 APK 上传到 Artifacts。

## 技术栈

Kotlin · Jetpack Compose (Material3) · MVVM + Repository · Room (KSP) · Koin · JSch · EncryptedSharedPreferences

## 贡献

欢迎 Issue 和 PR：BUG 反馈、功能建议、终端渲染改进、SFTP 上传实现、FreeRDP 内置 RDP 渲染集成等，都是对项目很大的帮助。

## 许可证

[MIT License](LICENSE)

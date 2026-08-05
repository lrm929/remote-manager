package com.remotemanager.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================================================
// 优雅深色 ·「午夜蓝」高级感配色
// 设计原则：深邃沉静的底色 + 单一精致主色，收敛高饱和霓虹，营造大气、专业氛围
// ============================================================================

// ---- 背景 / 面板 / 表面（由深到浅的层级）----
val TechBlack = Color(0xFF0D1117)           // 应用底色
val TechPanel = Color(0xFF141A24)           // 侧栏 / 顶栏 / 大面板
val TechSurface = Color(0xFF1E2636)         // 卡片 / 输入框（提亮）
val TechSurfaceElevated = Color(0xFF283346) // 浮起元素（大幅提亮）
val TechBorder = Color(0xFF354062)          // 细分隔线（更清晰可见）

// ---- 主色 / 功能色（收敛、精致，不再刺眼）----
val NeonCyan = Color(0xFF45D5FF)            // 主色：清亮的青
val NeonBlue = Color(0xFF5B8CFF)            // 蓝：信息 / SFTP
val NeonPurple = Color(0xFF9D7CFF)          // 紫：编辑 / 分组
val NeonPink = Color(0xFFFF5C8A)            // 品红：RDP
val NeonGreen = Color(0xFF3DDC97)           // 绿：SSH / 成功
val NeonYellow = Color(0xFFFFC94D)          // 黄：警告

val TextPrimary = Color(0xFFF0F4FC)         // 主文本：近白
val TextSecondary = Color(0xFFA8B4CE)       // 次文本（提亮）
val TextDisabled = Color(0xFF5E6A82)        // 禁用
val TextDisabledBackground = Color(0xFF283346)

// 终端
val TerminalBackground = Color(0xFF060910)
val TerminalText = Color(0xFF3DDC97)
val TerminalTextDim = Color(0xFF2BA877)
val TerminalCursor = Color(0xFF45D5FF)
val TerminalError = Color(0xFFFF5C6C)
val TerminalWarn = Color(0xFFFFC94D)

// 状态
val StatusConnected = Color(0xFF3DDC97)
val StatusDisconnected = Color(0xFFFF5C6C)
val StatusPending = Color(0xFFFFC94D)

package com.remotemanager.ui.screens.crt

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.remotemanager.data.model.ConnectionType
import com.remotemanager.data.model.Server
import com.remotemanager.ui.screens.ServerDetailScreen
import com.remotemanager.ui.screens.ServerEditScreen
import com.remotemanager.ui.screens.SftpBrowserScreen
import com.remotemanager.ui.screens.SshTerminalScreen
import com.remotemanager.ui.screens.rdp.RdpSessionScreen
import com.remotemanager.ui.theme.NeonBlue
import com.remotemanager.ui.theme.NeonCyan
import com.remotemanager.ui.theme.NeonGreen
import com.remotemanager.ui.theme.NeonPink
import com.remotemanager.ui.theme.NeonPurple
import com.remotemanager.ui.theme.TechBlack
import com.remotemanager.ui.theme.TechBorder
import com.remotemanager.ui.theme.TechPanel
import com.remotemanager.ui.theme.TechSurface
import com.remotemanager.ui.theme.TechSurfaceElevated
import com.remotemanager.ui.theme.TextDisabled
import com.remotemanager.ui.theme.TextPrimary
import com.remotemanager.ui.theme.TextSecondary
import com.remotemanager.ui.theme.TerminalError
import com.remotemanager.ui.viewmodel.ServerListUiState
import com.remotemanager.ui.viewmodel.ServerListViewModel
import org.koin.androidx.compose.koinViewModel

private enum class TabKind { DETAIL, SSH, SFTP, RDP, EDIT }

private data class SessionTab(
    val key: String,
    val kind: TabKind,
    val serverId: Long,
    val title: String
)

private const val DEFAULT_GROUP = "默认分组"
private val SIDEBAR_EXPANDED = 300.dp
private val SIDEBAR_COLLAPSED = 68.dp

/**
 * 大屏（平板横屏）桌面风格主页：左侧可折叠会话面板 + 顶部工具栏 + 右侧标签页会话区。
 * 采用优雅的「午夜蓝」深色主题，侧栏宽度使用平滑动画过渡。
 */
@Composable
fun CrtHomeScreen(
    viewModel: ServerListViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val tabs = remember { mutableStateListOf<SessionTab>() }
    var activeKey by rememberSaveable { mutableStateOf("") }
    var selectedServer by remember { mutableStateOf<Server?>(null) }
    var sidebarExpanded by rememberSaveable { mutableStateOf(true) }
    var collapsedGroups by remember { mutableStateOf(setOf<String>()) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val sidebarWidth by animateDpAsState(
        targetValue = if (sidebarExpanded) SIDEBAR_EXPANDED else SIDEBAR_COLLAPSED,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "sidebarWidth"
    )

    fun findServer(id: Long): Server? =
        uiState.servers.find { it.id == id } ?: selectedServer?.takeIf { it.id == id }

    fun closeTab(tabKey: String) {
        val index = tabs.indexOfFirst { it.key == tabKey }
        if (index < 0) return
        tabs.removeAt(index)
        if (activeKey == tabKey) {
            activeKey = when {
                tabs.isEmpty() -> ""
                index < tabs.size -> tabs[index].key
                else -> tabs[tabs.size - 1].key
            }
        }
    }

    fun openTab(kind: TabKind, serverId: Long) {
        val tabKey = "${kind.name}:$serverId"
        if (tabs.any { it.key == tabKey }) {
            activeKey = tabKey
            return
        }
        val name = findServer(serverId)?.name ?: ""
        val title = when (kind) {
            TabKind.DETAIL -> name.ifBlank { "详情" }
            TabKind.SSH -> if (name.isBlank()) "SSH" else "SSH $name"
            TabKind.SFTP -> if (name.isBlank()) "SFTP" else "SFTP $name"
            TabKind.RDP -> if (name.isBlank()) "RDP" else "RDP $name"
            TabKind.EDIT -> when {
                serverId == 0L -> "新建服务器"
                name.isBlank() -> "编辑"
                else -> "编辑 $name"
            }
        }
        tabs.add(SessionTab(key = tabKey, kind = kind, serverId = serverId, title = title))
        activeKey = tabKey
    }

    fun deleteSelectedServer() {
        val server = selectedServer ?: return
        viewModel.deleteServer(server)
        val removedKeys = tabs.filter { it.serverId == server.id }.map { it.key }
        tabs.removeAll { it.serverId == server.id }
        if (removedKeys.contains(activeKey)) {
            activeKey = tabs.lastOrNull()?.key ?: ""
        }
        selectedServer = null
        showDeleteDialog = false
    }

    LaunchedEffect(uiState.servers) {
        selectedServer?.let { sel ->
            uiState.servers.find { it.id == sel.id }?.let { selectedServer = it }
        }
        var i = 0
        while (i < tabs.size) {
            val tab = tabs[i]
            val server = uiState.servers.find { it.id == tab.serverId }
            if (server != null) {
                val newTitle = when (tab.kind) {
                    TabKind.DETAIL -> server.name
                    TabKind.SSH -> "SSH ${server.name}"
                    TabKind.SFTP -> "SFTP ${server.name}"
                    TabKind.RDP -> "RDP ${server.name}"
                    TabKind.EDIT -> "编辑 ${server.name}"
                }
                if (newTitle != tab.title) {
                    tabs[i] = tab.copy(title = newTitle)
                }
            }
            i++
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TechBlack)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // ── 左侧栏：宽度平滑动画，展开为完整面板、收起为图标栏 ──
            Column(
                modifier = Modifier
                    .width(sidebarWidth)
                    .fillMaxHeight()
                    .background(TechPanel)
                    .drawBehind {
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    NeonCyan.copy(alpha = 0.05f),
                                    Color.Transparent,
                                    NeonPurple.copy(alpha = 0.03f)
                                )
                            )
                        )
                    }
            ) {
                if (sidebarWidth > 180.dp) {
                    SessionPanel(
                        uiState = uiState,
                        selectedServerId = selectedServer?.id ?: 0L,
                        collapsedGroups = collapsedGroups,
                        onToggleGroup = { group ->
                            collapsedGroups = if (collapsedGroups.contains(group)) {
                                collapsedGroups - group
                            } else {
                                collapsedGroups + group
                            }
                        },
                        onServerClick = { server ->
                            selectedServer = server
                            when (server.type) {
                                ConnectionType.SSH -> openTab(TabKind.SSH, server.id)
                                ConnectionType.RDP -> openTab(TabKind.RDP, server.id)
                            }
                        },
                        onSearchQueryChanged = viewModel::onSearchQueryChanged,
                        onAddServer = { openTab(TabKind.EDIT, 0L) },
                        onCollapse = { sidebarExpanded = false }
                    )
                } else {
                    CollapsedRail(
                        servers = uiState.servers,
                        selectedServerId = selectedServer?.id ?: 0L,
                        onExpand = { sidebarExpanded = true },
                        onAddServer = { openTab(TabKind.EDIT, 0L) },
                        onServerClick = { server ->
                            selectedServer = server
                            when (server.type) {
                                ConnectionType.SSH -> openTab(TabKind.SSH, server.id)
                                ConnectionType.RDP -> openTab(TabKind.RDP, server.id)
                            }
                        }
                    )
                }
            }

            VerticalDivider(color = TechBorder, thickness = 1.dp)

            // ── 右侧主区 ──
            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                CrtToolbar(
                    selectedServer = selectedServer,
                    onNewServer = { openTab(TabKind.EDIT, 0L) },
                    onConnectSsh = { selectedServer?.let { openTab(TabKind.SSH, it.id) } },
                    onConnectRdp = { selectedServer?.let { openTab(TabKind.RDP, it.id) } },
                    onOpenSftp = { selectedServer?.let { openTab(TabKind.SFTP, it.id) } },
                    onEdit = { selectedServer?.let { openTab(TabKind.EDIT, it.id) } },
                    onDelete = { if (selectedServer != null) showDeleteDialog = true }
                )
                HorizontalDivider(color = TechBorder)

                if (tabs.isNotEmpty()) {
                    SessionTabStrip(
                        tabs = tabs,
                        activeKey = activeKey,
                        onSelect = { activeKey = it },
                        onClose = { closeTab(it) }
                    )
                    HorizontalDivider(color = TechBorder)
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    val activeTab = tabs.firstOrNull { it.key == activeKey }
                    if (activeTab == null) {
                        EmptySessionPlaceholder()
                    } else {
                        key(activeTab.key) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(TechPanel)
                                    .border(
                                        width = 1.dp,
                                        color = TechBorder,
                                        shape = RoundedCornerShape(16.dp)
                                    )
                            ) {
                                when (activeTab.kind) {
                                    TabKind.DETAIL -> ServerDetailScreen(
                                        serverId = activeTab.serverId,
                                        onNavigateBack = { closeTab(activeTab.key) },
                                        onEditClick = { openTab(TabKind.EDIT, activeTab.serverId) },
                                        onSshClick = { openTab(TabKind.SSH, activeTab.serverId) },
                                        onSftpClick = { openTab(TabKind.SFTP, activeTab.serverId) },
                                        onRdpLaunch = { openTab(TabKind.RDP, activeTab.serverId) }
                                    )
                                    TabKind.SSH -> SshTerminalScreen(
                                        serverId = activeTab.serverId,
                                        onNavigateBack = { closeTab(activeTab.key) }
                                    )
                                    TabKind.SFTP -> SftpBrowserScreen(
                                        serverId = activeTab.serverId,
                                        onNavigateBack = { closeTab(activeTab.key) }
                                    )
                                    TabKind.RDP -> RdpSessionScreen(
                                        serverId = activeTab.serverId,
                                        onNavigateBack = { closeTab(activeTab.key) }
                                    )
                                    TabKind.EDIT -> ServerEditScreen(
                                        serverId = activeTab.serverId,
                                        onNavigateBack = { closeTab(activeTab.key) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog && selectedServer != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除服务器") },
            text = { Text("确定要删除 \"${selectedServer?.name ?: ""}\" 吗？该服务器的所有会话标签也会被关闭。") },
            confirmButton = {
                TextButton(onClick = { deleteSelectedServer() }) {
                    Text("删除", color = TerminalError)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun BrandMark(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(34.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(NeonCyan, NeonBlue)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Terminal,
            contentDescription = null,
            tint = TechBlack,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun SessionPanel(
    uiState: ServerListUiState,
    selectedServerId: Long,
    collapsedGroups: Set<String>,
    onToggleGroup: (String) -> Unit,
    onServerClick: (Server) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onAddServer: () -> Unit,
    onCollapse: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // 头部：品牌标识 + 操作按钮
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(start = 16.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BrandMark()
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "REMOTE MANAGER",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary
                )
                Text(
                    text = "SSH · RDP · SFTP",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
            NeonIconButton(onClick = onAddServer) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "新建服务器",
                    modifier = Modifier.size(20.dp)
                )
            }
            NeonIconButton(onClick = onCollapse) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "收起面板",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        HorizontalDivider(color = TechBorder)

        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = onSearchQueryChanged,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            placeholder = { Text("过滤会话名…", color = TextSecondary) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = NeonCyan
                )
            },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = TechSurface,
                unfocusedContainerColor = TechSurface,
                focusedBorderColor = NeonCyan,
                unfocusedBorderColor = TechBorder,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            )
        )

        val grouped = uiState.servers
            .groupBy { it.group?.takeIf { g -> g.isNotBlank() } ?: DEFAULT_GROUP }
            .toSortedMap()

        if (uiState.servers.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "暂无会话",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 6.dp)
            ) {
                grouped.forEach { (groupName, serversInGroup) ->
                    item(key = "group:$groupName") {
                        GroupHeaderRow(
                            groupName = groupName,
                            count = serversInGroup.size,
                            expanded = !collapsedGroups.contains(groupName),
                            onToggle = { onToggleGroup(groupName) }
                        )
                    }
                    if (!collapsedGroups.contains(groupName)) {
                        items(serversInGroup, key = { "server:${it.id}" }) { server ->
                            ServerTreeRow(
                                server = server,
                                selected = server.id == selectedServerId,
                                onClick = { onServerClick(server) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupHeaderRow(
    groupName: String,
    count: Int,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
            .clickable(onClick = onToggle)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (expanded) {
                Icons.Default.KeyboardArrowDown
            } else {
                Icons.AutoMirrored.Filled.KeyboardArrowRight
            },
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = TextSecondary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = groupName,
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(TechSurfaceElevated)
                .padding(horizontal = 7.dp, vertical = 1.dp)
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun ServerTreeRow(
    server: Server,
    selected: Boolean,
    onClick: () -> Unit
) {
    val tint = when (server.type) {
        ConnectionType.RDP -> NeonPink
        ConnectionType.SSH -> NeonGreen
    }
    val bgColor = if (selected) NeonCyan.copy(alpha = 0.10f) else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(start = 12.dp, end = 12.dp, top = 9.dp, bottom = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 选中态左侧高亮指示条
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(28.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(if (selected) NeonCyan else Color.Transparent)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(tint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (server.type == ConnectionType.RDP) {
                    Icons.Default.Computer
                } else {
                    Icons.Default.Terminal
                },
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = tint
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = server.name,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${server.username}@${server.host}${server.displayPort}",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** 收起后的窄图标栏：展开按钮 + 新建 + 服务器快捷图标 */
@Composable
private fun CollapsedRail(
    servers: List<Server>,
    selectedServerId: Long,
    onExpand: () -> Unit,
    onAddServer: () -> Unit,
    onServerClick: (Server) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        NeonIconButton(onClick = onExpand) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "展开面板",
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        NeonIconButton(onClick = onAddServer) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "新建服务器",
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(
            color = TechBorder,
            modifier = Modifier.padding(horizontal = 14.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(servers, key = { "rail:${it.id}" }) { server ->
                val tint = when (server.type) {
                    ConnectionType.RDP -> NeonPink
                    ConnectionType.SSH -> NeonGreen
                }
                val selected = server.id == selectedServerId
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (selected) tint.copy(alpha = 0.16f) else Color.Transparent
                        )
                        .clickable { onServerClick(server) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (server.type == ConnectionType.RDP) {
                            Icons.Default.Computer
                        } else {
                            Icons.Default.Terminal
                        },
                        contentDescription = server.name,
                        modifier = Modifier.size(18.dp),
                        tint = tint
                    )
                }
            }
        }
    }
}

@Composable
private fun CrtToolbar(
    selectedServer: Server?,
    onNewServer: () -> Unit,
    onConnectSsh: () -> Unit,
    onConnectRdp: () -> Unit,
    onOpenSftp: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(TechPanel)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NeonIconButton(onClick = onNewServer) {
            Icon(Icons.Default.Add, contentDescription = "新建服务器")
        }
        VerticalDivider(
            color = TechBorder,
            modifier = Modifier
                .height(26.dp)
                .padding(horizontal = 6.dp)
        )
        NeonIconButton(
            onClick = onConnectSsh,
            enabled = selectedServer?.type == ConnectionType.SSH,
            tint = NeonGreen
        ) {
            Icon(Icons.Default.Terminal, contentDescription = "连接 SSH")
        }
        NeonIconButton(
            onClick = onConnectRdp,
            enabled = selectedServer?.type == ConnectionType.RDP,
            tint = NeonPink
        ) {
            Icon(Icons.Default.Computer, contentDescription = "连接远程桌面")
        }
        NeonIconButton(
            onClick = onOpenSftp,
            enabled = selectedServer?.type == ConnectionType.SSH,
            tint = NeonBlue
        ) {
            Icon(Icons.Default.Folder, contentDescription = "SFTP 文件")
        }
        VerticalDivider(
            color = TechBorder,
            modifier = Modifier
                .height(26.dp)
                .padding(horizontal = 6.dp)
        )
        NeonIconButton(onClick = onEdit, enabled = selectedServer != null) {
            Icon(Icons.Default.Edit, contentDescription = "编辑服务器")
        }
        NeonIconButton(
            onClick = onDelete,
            enabled = selectedServer != null,
            tint = TerminalError
        ) {
            Icon(Icons.Default.Delete, contentDescription = "删除服务器")
        }
        Spacer(modifier = Modifier.weight(1f))
        if (selectedServer != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .padding(end = 6.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(TechSurface)
                    .border(1.dp, TechBorder, RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            when (selectedServer.type) {
                                ConnectionType.SSH -> NeonGreen
                                ConnectionType.RDP -> NeonPink
                            },
                            shape = CircleShape
                        )
                )
                Text(
                    text = "${selectedServer.username}@${selectedServer.host}${selectedServer.displayPort}",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun SessionTabStrip(
    tabs: List<SessionTab>,
    activeKey: String,
    onSelect: (String) -> Unit,
    onClose: (String) -> Unit
) {
    val activeIndex = tabs.indexOfFirst { it.key == activeKey }.coerceAtLeast(0)
    ScrollableTabRow(
        selectedTabIndex = activeIndex,
        modifier = Modifier.fillMaxWidth(),
        containerColor = TechBlack,
        contentColor = NeonCyan,
        edgePadding = 12.dp,
        divider = {}
    ) {
        tabs.forEach { tab ->
            val selected = tab.key == activeKey
            val activeTint = tab.kind.statusTint()
            Tab(
                selected = selected,
                onClick = { onSelect(tab.key) },
                selectedContentColor = activeTint,
                unselectedContentColor = TextSecondary
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 6.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (selected) TechSurfaceElevated else Color.Transparent
                        )
                        .border(
                            width = 1.dp,
                            color = if (selected) TechBorder else Color.Transparent,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = tabIcon(tab.kind),
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = if (selected) activeTint else TextSecondary
                    )
                    Spacer(modifier = Modifier.width(7.dp))
                    Text(
                        text = tab.title,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (selected) TextPrimary else TextSecondary
                    )
                    Spacer(modifier = Modifier.width(7.dp))
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "关闭标签",
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .clickable { onClose(tab.key) },
                        tint = if (selected) TextSecondary else TextDisabled
                    )
                }
            }
        }
    }
}

private fun tabIcon(kind: TabKind): ImageVector = when (kind) {
    TabKind.DETAIL -> Icons.Default.Info
    TabKind.SSH -> Icons.Default.Terminal
    TabKind.SFTP -> Icons.Default.Folder
    TabKind.RDP -> Icons.Default.Computer
    TabKind.EDIT -> Icons.Default.Edit
}

private fun TabKind.statusTint(): Color = when (this) {
    TabKind.SSH -> NeonGreen
    TabKind.SFTP -> NeonBlue
    TabKind.RDP -> NeonPink
    TabKind.DETAIL -> NeonCyan
    TabKind.EDIT -> NeonPurple
}

@Composable
private fun EmptySessionPlaceholder() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(TechPanel)
                    .border(1.dp, TechBorder, RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Terminal,
                    contentDescription = null,
                    modifier = Modifier.size(44.dp),
                    tint = NeonCyan.copy(alpha = 0.5f)
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "从左侧选择一个服务器开始",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "SSH / RDP / SFTP 统一管理",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun NeonIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = NeonCyan,
    content: @Composable () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = IconButtonDefaults.iconButtonColors(
            contentColor = if (enabled) tint else TextDisabled,
            disabledContentColor = TextDisabled
        )
    ) {
        content()
    }
}

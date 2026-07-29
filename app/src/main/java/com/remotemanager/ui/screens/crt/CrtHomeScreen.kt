package com.remotemanager.ui.screens.crt

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.remotemanager.data.model.ConnectionType
import com.remotemanager.data.model.Server
import com.remotemanager.rdp.launchRdp
import com.remotemanager.ui.screens.ServerDetailScreen
import com.remotemanager.ui.screens.ServerEditScreen
import com.remotemanager.ui.screens.SftpBrowserScreen
import com.remotemanager.ui.screens.SshTerminalScreen
import com.remotemanager.ui.viewmodel.ServerListUiState
import com.remotemanager.ui.viewmodel.ServerListViewModel
import org.koin.androidx.compose.koinViewModel

private enum class TabKind { DETAIL, SSH, SFTP, EDIT }

private data class SessionTab(
    val key: String,
    val kind: TabKind,
    val serverId: Long,
    val title: String
)

private const val DEFAULT_GROUP = "默认分组"

/**
 * 大屏（平板横屏）桌面风格主页：左侧会话管理面板 + 顶部工具栏 + 右侧标签页会话区。
 */
@Composable
fun CrtHomeScreen(
    viewModel: ServerListViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // 标签列表与激活 key：进程重建丢失可接受，优先编译安全（不用 rememberSaveable 存集合）。
    val tabs = remember { mutableStateListOf<SessionTab>() }
    var activeKey by rememberSaveable { mutableStateOf("") }
    var selectedServer by remember { mutableStateOf<Server?>(null) }
    var panelCollapsed by rememberSaveable { mutableStateOf(false) }
    var collapsedGroups by remember { mutableStateOf(setOf<String>()) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    fun findServer(id: Long): Server? =
        uiState.servers.find { it.id == id }
            ?: selectedServer?.takeIf { it.id == id }

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

    // 服务器数据变化时同步选中对象与各标签标题（重命名后保持新鲜）。
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
                    TabKind.EDIT -> "编辑 ${server.name}"
                }
                if (newTitle != tab.title) {
                    tabs[i] = tab.copy(title = newTitle)
                }
            }
            i++
        }
    }

    Row(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (panelCollapsed) {
            CollapsedPanelStrip(onExpand = { panelCollapsed = false })
        } else {
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
                    openTab(TabKind.DETAIL, server.id)
                },
                onSearchQueryChanged = viewModel::onSearchQueryChanged,
                onAddServer = { openTab(TabKind.EDIT, 0L) },
                onCollapse = { panelCollapsed = true }
            )
        }
        VerticalDivider()

        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
            CrtToolbar(
                selectedServer = selectedServer,
                onNewServer = { openTab(TabKind.EDIT, 0L) },
                onConnectSsh = { selectedServer?.let { openTab(TabKind.SSH, it.id) } },
                onConnectRdp = { selectedServer?.let { launchRdp(context, it) } },
                onOpenSftp = { selectedServer?.let { openTab(TabKind.SFTP, it.id) } },
                onEdit = { selectedServer?.let { openTab(TabKind.EDIT, it.id) } },
                onDelete = { if (selectedServer != null) showDeleteDialog = true }
            )
            HorizontalDivider()

            if (tabs.isNotEmpty()) {
                SessionTabStrip(
                    tabs = tabs,
                    activeKey = activeKey,
                    onSelect = { activeKey = it },
                    onClose = { closeTab(it) }
                )
                HorizontalDivider()
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                val activeTab = tabs.firstOrNull { it.key == activeKey }
                if (activeTab == null) {
                    EmptySessionPlaceholder()
                } else {
                    key(activeTab.key) {
                        when (activeTab.kind) {
                            TabKind.DETAIL -> ServerDetailScreen(
                                serverId = activeTab.serverId,
                                onNavigateBack = { closeTab(activeTab.key) },
                                onEditClick = { openTab(TabKind.EDIT, activeTab.serverId) },
                                onSshClick = { openTab(TabKind.SSH, activeTab.serverId) },
                                onSftpClick = { openTab(TabKind.SFTP, activeTab.serverId) }
                            )
                            TabKind.SSH -> SshTerminalScreen(
                                serverId = activeTab.serverId,
                                onNavigateBack = { closeTab(activeTab.key) }
                            )
                            TabKind.SFTP -> SftpBrowserScreen(
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

    if (showDeleteDialog && selectedServer != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除服务器") },
            text = { Text("确定要删除 \"${selectedServer?.name ?: ""}\" 吗？该服务器的所有会话标签也会被关闭。") },
            confirmButton = {
                TextButton(onClick = { deleteSelectedServer() }) {
                    Text("删除")
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
    Column(
        modifier = Modifier
            .width(300.dp)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // 面板标题栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .padding(start = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "会话管理",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onAddServer) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "新建服务器",
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(onClick = onCollapse) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "收起面板",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        HorizontalDivider()

        // 过滤输入框
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = onSearchQueryChanged,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            placeholder = {
                Text("过滤会话名…", style = MaterialTheme.typography.bodyMedium)
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium
        )

        // 分组树
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
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
            .height(36.dp)
            .clickable(onClick = onToggle)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (expanded) {
                Icons.Default.KeyboardArrowDown
            } else {
                Icons.AutoMirrored.Filled.KeyboardArrowRight
            },
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
            imageVector = Icons.Default.Folder,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = groupName,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ServerTreeRow(
    server: Server,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(start = 30.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (server.type == ConnectionType.RDP) {
                Icons.Default.Computer
            } else {
                Icons.Default.Terminal
            },
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = if (selected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.primary
            }
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = server.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${server.username}@${server.host}${server.displayPort}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CollapsedPanelStrip(onExpand: () -> Unit) {
    Column(
        modifier = Modifier
            .width(40.dp)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onExpand)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "展开面板",
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        "会话管理".forEach { c ->
            Text(
                text = c.toString(),
                style = MaterialTheme.typography.labelMedium
            )
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
            .height(48.dp)
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onNewServer) {
            Icon(Icons.Default.Add, contentDescription = "新建服务器")
        }
        IconButton(
            onClick = onConnectSsh,
            enabled = selectedServer?.type == ConnectionType.SSH
        ) {
            Icon(Icons.Default.Terminal, contentDescription = "连接 SSH")
        }
        IconButton(
            onClick = onConnectRdp,
            enabled = selectedServer?.type == ConnectionType.RDP
        ) {
            Icon(Icons.Default.Computer, contentDescription = "连接远程桌面")
        }
        IconButton(
            onClick = onOpenSftp,
            enabled = selectedServer?.type == ConnectionType.SSH
        ) {
            Icon(Icons.Default.Folder, contentDescription = "SFTP 文件")
        }
        IconButton(onClick = onEdit, enabled = selectedServer != null) {
            Icon(Icons.Default.Edit, contentDescription = "编辑服务器")
        }
        IconButton(onClick = onDelete, enabled = selectedServer != null) {
            Icon(Icons.Default.Delete, contentDescription = "删除服务器")
        }
        Spacer(modifier = Modifier.weight(1f))
        if (selectedServer != null) {
            Text(
                text = "${selectedServer.username}@${selectedServer.host}${selectedServer.displayPort}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(end = 12.dp)
            )
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
        containerColor = MaterialTheme.colorScheme.surface,
        edgePadding = 0.dp
    ) {
        tabs.forEach { tab ->
            Tab(
                selected = tab.key == activeKey,
                onClick = { onSelect(tab.key) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = tabIcon(tab.kind),
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = tab.title,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "关闭标签",
                        modifier = Modifier
                            .size(14.dp)
                            .clickable { onClose(tab.key) }
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
    TabKind.EDIT -> Icons.Default.Edit
}

@Composable
private fun EmptySessionPlaceholder() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Terminal,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "从左侧会话管理中选择一个服务器",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

package com.remotemanager.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Monitor
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.remotemanager.R
import com.remotemanager.data.model.ConnectionType
import com.remotemanager.data.model.Server
import com.remotemanager.data.repository.ServerRepository
import com.remotemanager.ui.theme.NeonCyan
import com.remotemanager.ui.theme.NeonGreen
import com.remotemanager.ui.theme.NeonPink
import com.remotemanager.ui.theme.NeonPurple
import com.remotemanager.ui.theme.TechBorder
import com.remotemanager.ui.theme.TechPanel
import com.remotemanager.ui.theme.TechSurface
import com.remotemanager.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import org.koin.androidx.compose.get

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerDetailScreen(
    serverId: Long,
    onNavigateBack: () -> Unit,
    onEditClick: () -> Unit,
    onSshClick: () -> Unit,
    onSftpClick: () -> Unit,
    onRdpLaunch: () -> Unit,
    repository: ServerRepository = get()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var server by remember { mutableStateOf<Server?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(serverId) {
        server = repository.getServerById(serverId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(server?.name ?: stringResource(R.string.servers)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = onEditClick) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit_server))
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = TechPanel,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        server?.let { s ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ServerHeaderCard(server = s)
                InfoCard(server = s)
                ActionsCard(
                    server = s,
                    onRdpLaunch = onRdpLaunch,
                    onSshClick = onSshClick,
                    onSftpClick = onSftpClick
                )
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete)) },
            text = { Text(stringResource(R.string.confirm_delete, server?.name ?: "")) },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            server?.let { repository.deleteServer(it) }
                            Toast.makeText(context, "已删除", Toast.LENGTH_SHORT).show()
                            onNavigateBack()
                        }
                    }
                ) {
                    Text(stringResource(R.string.delete))
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
private fun ServerHeaderCard(server: Server) {
    val typeTint = if (server.type == ConnectionType.RDP) NeonPink else NeonGreen
    val typeLabel = if (server.type == ConnectionType.RDP) "RDP" else "SSH"
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, typeTint.copy(alpha = 0.35f), RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = TechSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            typeTint.copy(alpha = 0.12f),
                            Color.Transparent,
                            NeonPurple.copy(alpha = 0.04f)
                        )
                    )
                )
                .padding(22.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(typeTint.copy(alpha = 0.15f))
                        .border(1.dp, typeTint.copy(alpha = 0.35f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (server.type == ConnectionType.RDP) Icons.Default.Monitor else Icons.Default.Terminal,
                        contentDescription = null,
                        modifier = Modifier.size(30.dp),
                        tint = typeTint
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = server.name,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(typeTint.copy(alpha = 0.15f))
                                .border(1.dp, typeTint.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 9.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = typeLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = typeTint
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${server.username}@${server.host}${server.displayPort}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoCard(server: Server) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, TechBorder, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = TechSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            NeonCyan.copy(alpha = 0.05f),
                            Color.Transparent,
                            NeonPurple.copy(alpha = 0.03f)
                        )
                    )
                )
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            InfoRow(
                icon = if (server.type == ConnectionType.RDP) Icons.Default.Monitor else Icons.Default.Terminal,
                label = "类型",
                value = if (server.type == ConnectionType.RDP) "RDP" else "SSH",
                valueColor = if (server.type == ConnectionType.RDP) NeonPink else NeonGreen
            )
            InfoRow(icon = Icons.Default.Dns, label = "主机", value = "${server.host}:${server.port}")
            InfoRow(icon = Icons.Default.Person, label = "用户名", value = server.username.ifBlank { "-" })
            InfoRow(
                icon = if (!server.privateKey.isNullOrBlank()) Icons.Default.Key else Icons.Default.Lock,
                label = "认证",
                value = when {
                    !server.privateKey.isNullOrBlank() -> "私钥"
                    !server.password.isNullOrBlank() -> "密码"
                    else -> "未配置"
                }
            )
            if (!server.group.isNullOrBlank()) {
                InfoRow(icon = Icons.Default.Folder, label = "分组", value = server.group)
            }
            if (server.type == ConnectionType.RDP) {
                InfoRow(
                    icon = Icons.Default.Monitor,
                    label = "分辨率",
                    value = if (server.rdpWidth != null && server.rdpHeight != null) {
                        "${server.rdpWidth} x ${server.rdpHeight}"
                    } else "默认"
                )
                InfoRow(label = "颜色深度", value = "${server.rdpColorDepth} bit")
            }
            if (!server.description.isNullOrBlank()) {
                InfoRow(label = "备注", value = server.description)
            }
        }
    }
}

@Composable
private fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = TextSecondary
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = valueColor
        )
    }
}

@Composable
private fun ActionsCard(
    server: Server,
    onRdpLaunch: () -> Unit,
    onSshClick: () -> Unit,
    onSftpClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, TechBorder, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = TechSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            when (server.type) {
                ConnectionType.RDP -> {
                    TechButton(
                        onClick = onRdpLaunch,
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = NeonPink,
                        icon = Icons.Default.PlayArrow,
                        label = "连接远程桌面 (RDP)"
                    )
                }
                ConnectionType.SSH -> {
                    TechButton(
                        onClick = onSshClick,
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = NeonGreen,
                        icon = Icons.Default.Terminal,
                        label = "连接 SSH 终端"
                    )
                    OutlinedButton(
                        onClick = onSftpClick,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, TechBorder)
                    ) {
                        Icon(Icons.Default.Folder, contentDescription = null, tint = NeonBlue)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("打开 SFTP 文件管理", color = NeonBlue)
                    }
                }
            }
        }
    }
}

@Composable
private fun TechButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = NeonCyan,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    label: String = ""
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor.copy(alpha = 0.18f),
            contentColor = containerColor
        ),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = containerColor.copy(alpha = 0.50f)
        )
    ) {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(10.dp))
        }
        Text(label, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.weight(1f))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.NavigateNext,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = containerColor.copy(alpha = 0.7f)
        )
    }
}

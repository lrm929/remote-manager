package com.remotemanager.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.remotemanager.R
import com.remotemanager.data.model.ConnectionType
import com.remotemanager.data.model.Server
import com.remotemanager.data.repository.ServerRepository
import com.remotemanager.rdp.launchRdp
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
                }
            )
        }
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
                InfoCard(server = s)
                ActionsCard(
                    server = s,
                    onRdpLaunch = { launchRdp(context, s) },
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
private fun InfoCard(server: Server) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            InfoRow(label = "类型", value = if (server.type == ConnectionType.RDP) "RDP" else "SSH")
            InfoRow(label = "主机", value = "${server.host}:${server.port}")
            InfoRow(label = "用户名", value = server.username.ifBlank { "-" })
            InfoRow(label = "认证", value = when {
                !server.privateKey.isNullOrBlank() -> "私钥"
                !server.password.isNullOrBlank() -> "密码"
                else -> "未配置"
            })
            if (!server.group.isNullOrBlank()) {
                InfoRow(label = "分组", value = server.group)
            }
            if (!server.description.isNullOrBlank()) {
                InfoRow(label = "备注", value = server.description)
            }
            if (server.type == ConnectionType.RDP) {
                InfoRow(
                    label = "分辨率",
                    value = if (server.rdpWidth != null && server.rdpHeight != null) {
                        "${server.rdpWidth} x ${server.rdpHeight}"
                    } else "默认"
                )
                InfoRow(label = "颜色深度", value = "${server.rdpColorDepth} bit")
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "$label：",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
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
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (server.type) {
                ConnectionType.RDP -> {
                    Button(
                        onClick = onRdpLaunch,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("连接远程桌面 (RDP)")
                    }
                }
                ConnectionType.SSH -> {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = onSshClick,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("SSH 终端")
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        OutlinedButton(
                            onClick = onSftpClick,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("SFTP 文件")
                        }
                    }
                }
            }
        }
    }
}

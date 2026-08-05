package com.remotemanager.ui.screens.rdp

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.remotemanager.data.model.Server
import com.remotemanager.data.repository.ServerRepository
import androidx.core.content.FileProvider
import com.remotemanager.ui.theme.NeonPink
import com.remotemanager.ui.theme.TechBorder
import com.remotemanager.ui.theme.TechPanel
import com.remotemanager.ui.theme.TechSurface
import com.remotemanager.ui.theme.TerminalError
import com.remotemanager.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import org.koin.androidx.compose.get
import java.io.File

sealed class RdpUiState {
    data object Loading : RdpUiState()
    data class Ready(val server: Server) : RdpUiState()
    data class Error(val message: String) : RdpUiState()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RdpSessionScreen(
    serverId: Long,
    onNavigateBack: () -> Unit,
    repository: ServerRepository = get()
) {
    val context = LocalContext.current
    var uiState by remember { mutableStateOf<RdpUiState>(RdpUiState.Loading) }

    LaunchedEffect(serverId) {
        val server = repository.getServerById(serverId)
        uiState = if (server == null) {
            RdpUiState.Error("服务器不存在")
        } else if (server.type != com.remotemanager.data.model.ConnectionType.RDP) {
            RdpUiState.Error("不是 RDP 服务器")
        } else {
            RdpUiState.Ready(server)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("远程桌面", color = NeonPink) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = TechPanel,
                    titleContentColor = NeonPink
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(12.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(TechSurface)
                .border(1.dp, TechBorder, RoundedCornerShape(16.dp))
        ) {
            when (val state = uiState) {
                is RdpUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = NeonPink)
                    }
                }
                is RdpUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Computer,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = TerminalError
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = state.message,
                            color = TerminalError,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                is RdpUiState.Ready -> {
                    ExternalRdpLauncher(
                        server = state.server,
                        context = context
                    )
                }
            }
        }
    }
}

@Composable
private fun ExternalRdpLauncher(
    server: Server,
    context: Context
) {
    var launching by remember { mutableStateOf(false) }
    var showInstallOptions by remember { mutableStateOf(false) }

    if (showInstallOptions) {
        InstallClientPrompt(context = context)
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Computer,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = NeonPink.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "${server.username}@${server.host}:${server.port}",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "点击下方按钮启动远程桌面连接。\n需要先安装 aFreeRDP 或 Microsoft 远程桌面客户端。",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = {
                    launching = true
                    tryLaunchExternalRdp(context, server, onSuccess = { launching = false }, onNoClient = {
                        launching = false
                        showInstallOptions = true
                    })
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonPink.copy(alpha = 0.18f),
                    contentColor = NeonPink
                ),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, NeonPink.copy(alpha = 0.50f))
            ) {
                if (launching) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = NeonPink,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("启动 RDP 连接")
                }
            }
        }
    }
}

@Composable
private fun InstallClientPrompt(context: Context) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Computer,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = TerminalError
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "未找到 RDP 客户端",
            style = MaterialTheme.typography.titleMedium,
            color = TerminalError,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "请安装以下任一客户端后重试：",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.freerdp.afreerdp")))
                } catch (_: ActivityNotFoundException) {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.freerdp.afreerdp")))
                }
            },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NeonPink.copy(alpha = 0.15f), contentColor = NeonPink),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, TechBorder)
        ) {
            Text("安装 aFreeRDP（免费）", fontSize = 13.sp)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=ms.remote.desktop")))
                } catch (_: ActivityNotFoundException) {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=ms.remote.desktop")))
                }
            },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NeonPink.copy(alpha = 0.15f), contentColor = NeonPink),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, TechBorder)
        ) {
            Text("MS 远程桌面", fontSize = 13.sp)
        }
    }
}

private fun tryLaunchExternalRdp(
    context: Context,
    server: Server,
    onSuccess: () -> Unit,
    onNoClient: () -> Unit
) {
    // 方式1：尝试用 rdp:// URI 唤起
    val rdpUri = "rdp://${server.username}:${server.password}@${server.host}:${server.port}"
    val rdpIntent = Intent(Intent.ACTION_VIEW, Uri.parse(rdpUri)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    try {
        context.startActivity(rdpIntent)
        onSuccess()
        return
    } catch (_: ActivityNotFoundException) {
        // rdp:// 不被支持，尝试方式2
    }

    // 方式2：生成 .rdp 文件并打开
    try {
        val rdpFile = generateRdpFile(context, server)
        val fileUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            rdpFile
        )
        val fileIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(fileUri, "application/x-rdp")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(fileIntent)
        onSuccess()
    } catch (_: ActivityNotFoundException) {
        // 没有能打开 .rdp 文件的应用
        onNoClient()
    }
}

private fun generateRdpFile(context: Context, server: Server): File {
    val rdpContent = buildString {
        appendLine("full address:s:${server.host}:${server.port}")
        appendLine("username:s:${server.username}")
        if (!server.password.isNullOrBlank()) {
            appendLine("password:s:${server.password}")
        }
        appendLine("desktopwidth:i:${server.rdpWidth ?: 1280}")
        appendLine("desktopheight:i:${server.rdpHeight ?: 720}")
        appendLine("session bpp:i:${server.rdpColorDepth}")
        if (server.useNla) {
            appendLine("authentication level:i:2")
        }
        appendLine("compression:i:1")
        appendLine("displayconnectionbar:i:1")
    }

    val rdpDir = File(context.cacheDir, "rdp").also { it.mkdirs() }
    val rdpFile = File(rdpDir, "${server.name ?: "connection"}.rdp")
    rdpFile.writeText(rdpContent)
    return rdpFile
}

package com.remotemanager.ui.screens.rdp

import android.widget.Toast
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
import com.remotemanager.rdp.FreeRdpBridge
import com.remotemanager.rdp.launchRdp
import com.remotemanager.ui.theme.NeonPink
import com.remotemanager.ui.theme.TechBorder
import com.remotemanager.ui.theme.TechPanel
import com.remotemanager.ui.theme.TechSurface
import com.remotemanager.ui.theme.TerminalError
import com.remotemanager.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import org.koin.androidx.compose.get

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
                    BuiltInRdpPlaceholder(
                        server = state.server,
                        onLaunchExternal = {
                            launchRdp(context, state.server)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun BuiltInRdpPlaceholder(
    server: Server,
    onLaunchExternal: () -> Unit
) {
    var connecting by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(connecting) {
        if (connecting) {
            delay(400)
            val result = FreeRdpBridge.connect(
                host = server.host,
                port = server.port,
                username = server.username,
                password = server.password,
                domain = null,
                width = server.rdpWidth ?: 1280,
                height = server.rdpHeight ?: 720,
                colorDepth = server.rdpColorDepth,
                useNla = server.useNla
            )
            result.onSuccess {
                // TODO: 当 FreeRDP native 库就绪后，在此处切换到 SurfaceView 渲染。
            }.onFailure { error ->
                Toast.makeText(
                    context,
                    "内置 RDP 引擎未就绪：${error.message}，尝试唤起外部客户端",
                    Toast.LENGTH_LONG
                ).show()
                onLaunchExternal()
            }
            connecting = false
        }
    }

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
            text = "内置 RDP 客户端正在接入 FreeRDP 原生渲染引擎。\n当前版本先通过外部客户端完成连接，后续更新将完全在 App 内渲染远程桌面。",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = { connecting = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = NeonPink.copy(alpha = 0.18f),
                contentColor = NeonPink
            ),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                NeonPink.copy(alpha = 0.50f)
            )
        ) {
            if (connecting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = NeonPink,
                    strokeWidth = 2.dp
                )
            } else {
                Text("连接远程桌面")
            }
        }
    }
}

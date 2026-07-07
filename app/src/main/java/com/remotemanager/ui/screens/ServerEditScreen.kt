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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.remotemanager.R
import com.remotemanager.data.model.ConnectionType
import com.remotemanager.data.model.Server
import com.remotemanager.data.repository.ServerRepository
import kotlinx.coroutines.launch
import org.koin.androidx.compose.get

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerEditScreen(
    serverId: Long,
    onNavigateBack: () -> Unit,
    repository: ServerRepository = get()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isNew by remember { mutableStateOf(serverId == 0L) }

    var name by remember { mutableStateOf("") }
    var host by remember { mutableStateOf("") }
    var portText by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var privateKey by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(ConnectionType.SSH) }
    var group by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var rdpWidthText by remember { mutableStateOf("") }
    var rdpHeightText by remember { mutableStateOf("") }
    var rdpColorDepth by remember { mutableIntStateOf(32) }
    var useNla by remember { mutableStateOf(true) }

    var usePrivateKey by remember { mutableStateOf(false) }

    LaunchedEffect(serverId) {
        if (serverId != 0L) {
            repository.getServerById(serverId)?.let { s ->
                isNew = false
                name = s.name
                host = s.host
                portText = if (s.port == s.defaultPort) "" else s.port.toString()
                username = s.username
                password = s.password ?: ""
                privateKey = s.privateKey ?: ""
                type = s.type
                group = s.group ?: ""
                description = s.description ?: ""
                rdpWidthText = s.rdpWidth?.toString() ?: ""
                rdpHeightText = s.rdpHeight?.toString() ?: ""
                rdpColorDepth = s.rdpColorDepth
                useNla = s.useNla
                usePrivateKey = !s.privateKey.isNullOrBlank()
            }
        }
    }

    fun save() {
        if (name.isBlank() || host.isBlank()) {
            Toast.makeText(context, "名称和主机地址不能为空", Toast.LENGTH_SHORT).show()
            return
        }
        val port = portText.toIntOrNull() ?: type.defaultPort
        val width = rdpWidthText.toIntOrNull()
        val height = rdpHeightText.toIntOrNull()

        val server = Server(
            id = serverId,
            name = name.trim(),
            host = host.trim(),
            port = port,
            username = username.trim(),
            password = if (usePrivateKey) null else password,
            privateKey = if (usePrivateKey) privateKey else null,
            type = type,
            group = group.trim().ifBlank { null },
            description = description.trim().ifBlank { null },
            rdpWidth = width,
            rdpHeight = height,
            rdpColorDepth = rdpColorDepth,
            useNla = useNla
        )

        scope.launch {
            repository.saveServer(server)
            Toast.makeText(context, "已保存", Toast.LENGTH_SHORT).show()
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isNew) stringResource(R.string.add_server) else stringResource(R.string.edit_server)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.name)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            TypeSelector(type = type, onTypeSelected = {
                type = it
                if (portText.isBlank()) {
                    portText = it.defaultPort.toString()
                }
            })

            OutlinedTextField(
                value = host,
                onValueChange = { host = it },
                label = { Text(stringResource(R.string.host)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = portText,
                onValueChange = { portText = it.filter { c -> c.isDigit() } },
                label = { Text(stringResource(R.string.port)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text(type.defaultPort.toString()) }
            )

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text(stringResource(R.string.username)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            AuthSelector(
                usePrivateKey = usePrivateKey,
                onToggle = { usePrivateKey = it }
            )

            if (usePrivateKey) {
                OutlinedTextField(
                    value = privateKey,
                    onValueChange = { privateKey = it },
                    label = { Text(stringResource(R.string.private_key)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4,
                    maxLines = 8
                )
            } else {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.password)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            OutlinedTextField(
                value = group,
                onValueChange = { group = it },
                label = { Text(stringResource(R.string.group)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(stringResource(R.string.description)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4
            )

            if (type == ConnectionType.RDP) {
                RdpOptions(
                    widthText = rdpWidthText,
                    onWidthChange = { rdpWidthText = it },
                    heightText = rdpHeightText,
                    onHeightChange = { rdpHeightText = it },
                    colorDepth = rdpColorDepth,
                    onColorDepthChange = { rdpColorDepth = it },
                    useNla = useNla,
                    onNlaChange = { useNla = it }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = ::save,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.save))
            }
        }
    }
}

@Composable
private fun TypeSelector(
    type: ConnectionType,
    onTypeSelected: (ConnectionType) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        ConnectionType.entries.forEach { option ->
            Row(
                modifier = Modifier
                    .weight(1f)
                    .selectable(
                        selected = type == option,
                        onClick = { onTypeSelected(option) },
                        role = Role.RadioButton
                    )
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = type == option,
                    onClick = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when (option) {
                        ConnectionType.RDP -> stringResource(R.string.rdp)
                        ConnectionType.SSH -> stringResource(R.string.ssh)
                    }
                )
            }
        }
    }
}

@Composable
private fun AuthSelector(
    usePrivateKey: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("使用私钥认证")
        Switch(checked = usePrivateKey, onCheckedChange = onToggle)
    }
}

@Composable
private fun RdpOptions(
    widthText: String,
    onWidthChange: (String) -> Unit,
    heightText: String,
    onHeightChange: (String) -> Unit,
    colorDepth: Int,
    onColorDepthChange: (Int) -> Unit,
    useNla: Boolean,
    onNlaChange: (Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("RDP 选项", style = MaterialTheme.typography.titleSmall)

        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = widthText,
                onValueChange = { onWidthChange(it.filter { c -> c.isDigit() }) },
                label = { Text("宽度") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            Spacer(modifier = Modifier.width(12.dp))
            OutlinedTextField(
                value = heightText,
                onValueChange = { onHeightChange(it.filter { c -> c.isDigit() }) },
                label = { Text("高度") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = colorDepth.toString(),
                onValueChange = {
                    onColorDepthChange(it.toIntOrNull() ?: 32)
                },
                label = { Text("颜色深度") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("启用 NLA 网络级认证")
            Switch(checked = useNla, onCheckedChange = onNlaChange)
        }
    }
}

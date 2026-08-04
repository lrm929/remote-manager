package com.remotemanager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.remotemanager.ui.theme.NeonBlue
import com.remotemanager.ui.theme.NeonCyan
import com.remotemanager.ui.theme.NeonGreen
import com.remotemanager.ui.theme.NeonPink
import com.remotemanager.ui.theme.NeonPurple
import com.remotemanager.ui.theme.TechBorder
import com.remotemanager.ui.theme.TechPanel
import com.remotemanager.ui.theme.TerminalBackground
import com.remotemanager.ui.theme.TerminalCursor
import com.remotemanager.ui.theme.TerminalError
import com.remotemanager.ui.theme.TerminalText
import com.remotemanager.ui.theme.TextSecondary
import com.remotemanager.ui.viewmodel.SshUiState
import com.remotemanager.ui.viewmodel.SshViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SshTerminalScreen(
    serverId: Long,
    onNavigateBack: () -> Unit,
    viewModel: SshViewModel = koinViewModel(key = "ssh-$serverId") { parametersOf(serverId) }
) {
    val uiState by viewModel.uiState.collectAsState()
    val terminalText by viewModel.terminalText.collectAsState()
    val listState = rememberLazyListState()
    val lines = remember(terminalText) { terminalText.split("\n") }

    LaunchedEffect(lines.size) {
        if (lines.isNotEmpty()) {
            listState.animateScrollToItem(lines.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (val state = uiState) {
                            is SshUiState.Connected -> state.serverName
                            is SshUiState.Error -> "SSH - 错误"
                            else -> "SSH 终端"
                        },
                        color = NeonGreen
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    when (uiState) {
                        is SshUiState.Connected -> {
                            IconButton(onClick = viewModel::disconnect) {
                                Text("断开", fontSize = 12.sp, color = TerminalError)
                            }
                        }
                        is SshUiState.Disconnected, is SshUiState.Error -> {
                            IconButton(onClick = viewModel::connect) {
                                Text("重连", fontSize = 12.sp, color = NeonGreen)
                            }
                        }
                        else -> {}
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = TechPanel,
                    titleContentColor = NeonGreen
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(TerminalBackground)
                    .border(1.dp, TechBorder, RoundedCornerShape(12.dp))
            ) {
                when (uiState) {
                    is SshUiState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = TerminalText)
                        }
                    }
                    is SshUiState.Error -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (uiState as SshUiState.Error).message,
                                color = TerminalError,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                    else -> {
                        TerminalContent(
                            lines = lines,
                            listState = listState,
                            enabled = uiState is SshUiState.Connected,
                            onKeyEvent = { event ->
                                viewModel.onTerminalKeyEvent(event)
                            },
                            onSendRaw = { data ->
                                viewModel.sendRaw(data)
                            }
                        )
                    }
                }
            }

            if (uiState is SshUiState.Connected) {
                Spacer(modifier = Modifier.height(8.dp))
                ShortcutBar(
                    onSendRaw = viewModel::sendRaw,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun TerminalContent(
    lines: List<String>,
    listState: androidx.compose.foundation.lazy.LazyListState,
    enabled: Boolean,
    onKeyEvent: (android.view.KeyEvent) -> Boolean,
    onSendRaw: (String) -> Unit
) {
    var input by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(enabled) {
        if (enabled) {
            focusRequester.requestFocus()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(12.dp)
        ) {
            items(lines) { line ->
                Text(
                    text = line,
                    color = TerminalText,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (enabled) {
            // Inline terminal input line: looks like a real shell prompt.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TerminalBackground)
                    .border(
                        width = 1.dp,
                        color = NeonGreen.copy(alpha = 0.30f),
                        shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .clickable { focusRequester.requestFocus() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$ ",
                    color = NeonGreen,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    lineHeight = 18.sp
                )
                BasicTextField(
                    value = input,
                    onValueChange = { newValue ->
                        if (newValue.length > input.length) {
                            val added = newValue.substring(input.length)
                            added.forEach { char ->
                                onSendRaw(char.toString())
                            }
                        } else if (newValue.length < input.length) {
                            repeat(input.length - newValue.length) {
                                onSendRaw("\u007F")
                            }
                        }
                        input = newValue
                    },
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester)
                        .onKeyEvent { event ->
                            val handled = onKeyEvent(event.nativeKeyEvent)
                            if (handled && event.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_ENTER) {
                                input = ""
                            }
                            handled
                        },
                    textStyle = TextStyle(
                        color = TerminalText,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        lineHeight = 18.sp
                    ),
                    cursorBrush = SolidColor(TerminalCursor),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Ascii,
                        imeAction = ImeAction.Send
                    ),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            onSendRaw("\r")
                            input = ""
                        }
                    ),
                    singleLine = true
                )
            }
        }
    }
}

@Composable
private fun ShortcutBar(
    onSendRaw: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(TechPanel)
            .border(1.dp, TechBorder, RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ShortcutButton(text = "Ctrl+C", color = NeonPink, onClick = { onSendRaw("\u0003") })
        ShortcutButton(text = "Ctrl+D", color = NeonPurple, onClick = { onSendRaw("\u0004") })
        ShortcutButton(text = "Ctrl+Z", color = NeonBlue, onClick = { onSendRaw("\u001A") })
        ShortcutButton(text = "Tab", color = NeonCyan, onClick = { onSendRaw("\t") })
        ShortcutButton(text = "Clear", color = TerminalText, onClick = { onSendRaw("clear\n") })
    }
}

@Composable
private fun ShortcutButton(
    text: String,
    color: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.40f), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            color = color
        )
    }
}

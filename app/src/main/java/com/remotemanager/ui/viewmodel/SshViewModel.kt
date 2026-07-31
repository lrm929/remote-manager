package com.remotemanager.ui.viewmodel

import android.view.KeyEvent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.remotemanager.data.model.Server
import com.remotemanager.data.repository.ServerRepository
import com.remotemanager.ssh.SshConnection
import com.remotemanager.ssh.SshSessionManager
import com.remotemanager.ssh.TerminalEmulator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class SshUiState {
    data object Loading : SshUiState()
    data object Disconnected : SshUiState()
    data class Error(val message: String) : SshUiState()
    data class Connected(val serverName: String) : SshUiState()
}

class SshViewModel(
    private val serverId: Long,
    private val repository: ServerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<SshUiState>(SshUiState.Loading)
    val uiState: StateFlow<SshUiState> = _uiState

    private val _terminalText = MutableStateFlow("")
    val terminalText: StateFlow<String> = _terminalText

    private val terminalEmulator = TerminalEmulator()
    private var connection: SshConnection? = null

    init {
        connect()
    }

    fun connect() {
        viewModelScope.launch {
            _uiState.value = SshUiState.Loading
            val server = repository.getServerById(serverId)
            if (server == null) {
                _uiState.value = SshUiState.Error("服务器不存在")
                return@launch
            }
            if (server.type != com.remotemanager.data.model.ConnectionType.SSH) {
                _uiState.value = SshUiState.Error("不是 SSH 服务器")
                return@launch
            }

            val conn = SshConnection(server)
            connection = conn
            SshSessionManager.setConnection(conn)

            val result = conn.connect()
            result.onSuccess {
                _uiState.value = SshUiState.Connected(server.name)
                startReadLoop(conn, server)
            }.onFailure { e ->
                _uiState.value = SshUiState.Error(e.message ?: "连接失败")
            }
        }
    }

    private fun startReadLoop(conn: SshConnection, server: Server) {
        viewModelScope.launch {
            conn.readLoop(
                onOutput = { output ->
                    terminalEmulator.append(output)
                    _terminalText.value = terminalEmulator.text.text
                },
                onError = { error ->
                    _uiState.value = SshUiState.Error(error)
                },
                onDisconnect = {
                    _uiState.value = SshUiState.Disconnected
                }
            )
        }
    }

    /**
     * 处理终端直接输入。支持普通字符、回车、退格、Tab、方向键以及 Ctrl 组合键。
     */
    fun onTerminalKeyEvent(event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN) return false

        val data = when {
            // Ctrl + C / D / Z etc.
            event.isCtrlPressed && event.keyCode == KeyEvent.KEYCODE_C -> "\u0003"
            event.isCtrlPressed && event.keyCode == KeyEvent.KEYCODE_D -> "\u0004"
            event.isCtrlPressed && event.keyCode == KeyEvent.KEYCODE_Z -> "\u001A"
            event.isCtrlPressed && event.keyCode == KeyEvent.KEYCODE_A -> "\u0001"
            event.isCtrlPressed && event.keyCode == KeyEvent.KEYCODE_E -> "\u0005"
            event.isCtrlPressed && event.keyCode == KeyEvent.KEYCODE_L -> "\u000C"
            event.isCtrlPressed && event.keyCode == KeyEvent.KEYCODE_U -> "\u0015"

            // Functional keys
            event.keyCode == KeyEvent.KEYCODE_ENTER -> "\r"
            event.keyCode == KeyEvent.KEYCODE_DEL -> "\u007F"
            event.keyCode == KeyEvent.KEYCODE_FORWARD_DEL -> "\u001B[3~"
            event.keyCode == KeyEvent.KEYCODE_TAB -> "\t"
            event.keyCode == KeyEvent.KEYCODE_ESCAPE -> "\u001B"
            event.keyCode == KeyEvent.KEYCODE_SPACE -> " "

            // Arrow keys
            event.keyCode == KeyEvent.KEYCODE_DPAD_UP -> "\u001B[A"
            event.keyCode == KeyEvent.KEYCODE_DPAD_DOWN -> "\u001B[B"
            event.keyCode == KeyEvent.KEYCODE_DPAD_RIGHT -> "\u001B[C"
            event.keyCode == KeyEvent.KEYCODE_DPAD_LEFT -> "\u001B[D"
            event.keyCode == KeyEvent.KEYCODE_MOVE_HOME -> "\u001B[H"
            event.keyCode == KeyEvent.KEYCODE_MOVE_END -> "\u001B[F"

            // Page up / down
            event.keyCode == KeyEvent.KEYCODE_PAGE_UP -> "\u001B[5~"
            event.keyCode == KeyEvent.KEYCODE_PAGE_DOWN -> "\u001B[6~"

            // Printable character
            event.unicodeChar != 0 -> event.unicodeChar.toChar().toString()
            else -> null
        }

        return data?.let {
            sendRaw(it)
            true
        } ?: false
    }

    fun sendCommand(command: String) {
        viewModelScope.launch(Dispatchers.IO) {
            connection?.write(command)
            if (!command.endsWith("\n") && !command.endsWith("\r")) {
                connection?.write("\n")
            }
        }
    }

    fun sendRaw(data: String) {
        viewModelScope.launch(Dispatchers.IO) {
            connection?.write(data)
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            connection?.disconnect()
            SshSessionManager.disconnect()
            _uiState.value = SshUiState.Disconnected
        }
    }

    override fun onCleared() {
        super.onCleared()
        connection?.disconnect()
    }
}

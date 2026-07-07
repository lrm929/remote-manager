package com.remotemanager.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.remotemanager.data.model.Server
import com.remotemanager.data.repository.ServerRepository
import com.remotemanager.ssh.SshConnection
import com.remotemanager.ssh.SshSessionManager
import com.remotemanager.ssh.TerminalEmulator
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

    fun sendCommand(command: String) {
        connection?.write(command)
        if (!command.endsWith("\n") && !command.endsWith("\r")) {
            connection?.write("\n")
        }
    }

    fun sendRaw(data: String) {
        connection?.write(data)
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

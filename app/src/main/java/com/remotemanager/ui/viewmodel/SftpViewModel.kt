package com.remotemanager.ui.viewmodel

import android.content.Context
import android.os.Environment
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import com.jcraft.jsch.SftpATTRS
import com.remotemanager.data.model.ConnectionType
import com.remotemanager.data.repository.ServerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Properties

data class SftpFile(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long,
    val lastModified: Long
)

sealed class SftpUiState {
    data object Loading : SftpUiState()
    data class Error(val message: String) : SftpUiState()
    data class Success(
        val currentPath: String,
        val files: List<SftpFile>
    ) : SftpUiState()
}

class SftpViewModel(
    private val serverId: Long,
    private val repository: ServerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<SftpUiState>(SftpUiState.Loading)
    val uiState: StateFlow<SftpUiState> = _uiState

    private var session: Session? = null
    private var channel: ChannelSftp? = null

    init {
        connect()
    }

    fun connect() {
        viewModelScope.launch {
            _uiState.value = SftpUiState.Loading
            val server = repository.getServerById(serverId)
            if (server == null) {
                _uiState.value = SftpUiState.Error("服务器不存在")
                return@launch
            }
            if (server.type != ConnectionType.SSH) {
                _uiState.value = SftpUiState.Error("不是 SSH 服务器")
                return@launch
            }

            withContext(Dispatchers.IO) {
                try {
                    val jsch = JSch()
                    val session = jsch.getSession(server.username, server.host, server.port).apply {
                        setConfig(Properties().apply {
                            put("StrictHostKeyChecking", "no")
                            put("PreferredAuthentications", "publickey,password")
                        })
                        timeout = 15000

                        if (!server.privateKey.isNullOrBlank()) {
                            jsch.addIdentity("sftp_${server.id}", server.privateKey.toByteArray(), null, null)
                        } else if (!server.password.isNullOrBlank()) {
                            setPassword(server.password)
                        }
                    }
                    session.connect()
                    this@SftpViewModel.session = session

                    val channel = session.openChannel("sftp") as ChannelSftp
                    channel.connect()
                    this@SftpViewModel.channel = channel

                    listFiles(channel.pwd())
                } catch (e: Exception) {
                    _uiState.value = SftpUiState.Error(e.message ?: "SFTP 连接失败")
                }
            }
        }
    }

    fun navigateTo(path: String) {
        val ch = channel ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                ch.cd(path)
                listFiles(ch.pwd())
            } catch (e: Exception) {
                _uiState.value = SftpUiState.Error(e.message ?: "进入目录失败")
            }
        }
    }

    fun navigateUp() {
        val ch = channel ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                ch.cd("..")
                listFiles(ch.pwd())
            } catch (e: Exception) {
                _uiState.value = SftpUiState.Error(e.message ?: "返回上级失败")
            }
        }
    }

    fun downloadFile(file: SftpFile, context: Context) {
        val ch = channel ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val localFile = File(downloadsDir, file.name)
                ch.get(file.path, localFile.absolutePath)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "已下载到 ${localFile.absolutePath}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "下载失败: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun listFiles(path: String) {
        val ch = channel ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val vector = ch.ls(path)
                val files = vector.mapNotNull { item ->
                    val entry = item as com.jcraft.jsch.ChannelSftp.LsEntry
                    val attrs = entry.attrs
                    if (entry.filename == "." || entry.filename == "..") return@mapNotNull null
                    SftpFile(
                        name = entry.filename,
                        path = "$path/${entry.filename}",
                        isDirectory = attrs.isDir,
                        size = attrs.size,
                        lastModified = attrs.mTime.toLong() * 1000
                    )
                }.sortedWith(compareByDescending<SftpFile> { it.isDirectory }.thenBy { it.name })

                withContext(Dispatchers.Main) {
                    _uiState.value = SftpUiState.Success(currentPath = path, files = files)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.value = SftpUiState.Error(e.message ?: "列出文件失败")
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        try { channel?.disconnect() } catch (_: Exception) {}
        try { session?.disconnect() } catch (_: Exception) {}
    }
}

package com.remotemanager.ssh

import com.jcraft.jsch.ChannelShell
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import com.remotemanager.data.model.Server
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.util.Properties

class SshConnection(
    private val server: Server
) {

    private var session: Session? = null
    private var channel: ChannelShell? = null
    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null

    var isConnected: Boolean = false
        private set

    @Suppress("DEPRECATION")
    suspend fun connect(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val jsch = JSch()
            val session = jsch.getSession(server.username, server.host, server.port).apply {
                setConfig(Properties().apply {
                    put("StrictHostKeyChecking", "no")
                    put("PreferredAuthentications", "publickey,password")
                })
                timeout = 15000

                if (!server.privateKey.isNullOrBlank()) {
                    jsch.addIdentity("server_${server.id}", server.privateKey.toByteArray(), null, null)
                } else if (!server.password.isNullOrBlank()) {
                    setPassword(server.password)
                }
            }

            session.connect()
            this@SshConnection.session = session

            val channel = session.openChannel("shell") as ChannelShell
            channel.setPtyType("xterm-256color")
            outputStream = channel.outputStream
            inputStream = channel.inputStream
            channel.connect()
            this@SshConnection.channel = channel
            isConnected = true

            Result.success(Unit)
        } catch (e: Exception) {
            disconnect()
            Result.failure(e)
        }
    }

    fun write(data: String) {
        try {
            outputStream?.write(data.toByteArray())
            outputStream?.flush()
        } catch (_: Exception) {
            isConnected = false
        }
    }

    suspend fun readLoop(
        onOutput: (String) -> Unit,
        onError: (String) -> Unit,
        onDisconnect: () -> Unit
    ) = withContext(Dispatchers.IO) {
        val buffer = ByteArray(8192)
        val stream = inputStream ?: run {
            onDisconnect()
            return@withContext
        }
        try {
            while (isConnected) {
                val available = stream.available()
                if (available > 0) {
                    val read = stream.read(buffer, 0, minOf(available, buffer.size))
                    if (read > 0) {
                        val text = String(buffer, 0, read)
                        onOutput(text)
                    }
                } else {
                    if (channel?.isClosed == true) {
                        break
                    }
                    Thread.sleep(50)
                }
            }
        } catch (e: Exception) {
            onError(e.message ?: "Read error")
        } finally {
            isConnected = false
            onDisconnect()
        }
    }

    fun disconnect() {
        isConnected = false
        try { outputStream?.close() } catch (_: Exception) {}
        try { inputStream?.close() } catch (_: Exception) {}
        try { channel?.disconnect() } catch (_: Exception) {}
        try { session?.disconnect() } catch (_: Exception) {}
        channel = null
        session = null
        outputStream = null
        inputStream = null
    }
}

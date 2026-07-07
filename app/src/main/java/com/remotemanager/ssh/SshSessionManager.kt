package com.remotemanager.ssh

object SshSessionManager {

    private var connection: SshConnection? = null

    fun setConnection(conn: SshConnection?) {
        connection?.disconnect()
        connection = conn
    }

    fun getConnection(): SshConnection? = connection

    fun disconnect() {
        connection?.disconnect()
        connection = null
    }
}

package com.remotemanager.data.model

data class Server(
    val id: Long = 0,
    val name: String,
    val host: String,
    val port: Int = 22,
    val username: String = "",
    val password: String? = null,
    val privateKey: String? = null,
    val type: ConnectionType = ConnectionType.SSH,
    val group: String? = null,
    val description: String? = null,
    val rdpWidth: Int? = null,
    val rdpHeight: Int? = null,
    val rdpColorDepth: Int = 32,
    val useNla: Boolean = true
) {
    val displayPort: String
        get() = when (type) {
            ConnectionType.RDP -> if (port == 3389) "" else ":$port"
            ConnectionType.SSH -> if (port == 22) "" else ":$port"
        }

    val defaultPort: Int
        get() = when (type) {
            ConnectionType.RDP -> 3389
            ConnectionType.SSH -> 22
        }
}

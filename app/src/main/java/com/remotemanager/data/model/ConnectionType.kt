package com.remotemanager.data.model

enum class ConnectionType(val defaultPort: Int) {
    RDP(3389),
    SSH(22)
}

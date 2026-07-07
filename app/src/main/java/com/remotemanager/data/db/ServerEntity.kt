package com.remotemanager.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.remotemanager.data.model.ConnectionType
import com.remotemanager.data.model.Server

@Entity(tableName = "servers")
data class ServerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val host: String,
    val port: Int,
    val username: String,
    val passwordEncrypted: String?,
    val privateKeyEncrypted: String?,
    val type: String,
    val groupName: String?,
    val description: String?,
    val rdpWidth: Int?,
    val rdpHeight: Int?,
    val rdpColorDepth: Int,
    val useNla: Boolean
) {
    fun toServer(password: String?, privateKey: String?): Server = Server(
        id = id,
        name = name,
        host = host,
        port = port,
        username = username,
        password = password,
        privateKey = privateKey,
        type = ConnectionType.valueOf(type),
        group = groupName,
        description = description,
        rdpWidth = rdpWidth,
        rdpHeight = rdpHeight,
        rdpColorDepth = rdpColorDepth,
        useNla = useNla
    )

    companion object {
        fun fromServer(server: Server, passwordEncrypted: String?, privateKeyEncrypted: String?): ServerEntity = ServerEntity(
            id = server.id,
            name = server.name,
            host = server.host,
            port = server.port,
            username = server.username,
            passwordEncrypted = passwordEncrypted,
            privateKeyEncrypted = privateKeyEncrypted,
            type = server.type.name,
            groupName = server.group,
            description = server.description,
            rdpWidth = server.rdpWidth,
            rdpHeight = server.rdpHeight,
            rdpColorDepth = server.rdpColorDepth,
            useNla = server.useNla
        )
    }
}

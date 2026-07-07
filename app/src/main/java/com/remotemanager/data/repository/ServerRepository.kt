package com.remotemanager.data.repository

import com.remotemanager.data.db.ServerDao
import com.remotemanager.data.db.ServerEntity
import com.remotemanager.data.model.Server
import com.remotemanager.data.security.CryptoManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ServerRepository(
    private val serverDao: ServerDao,
    private val cryptoManager: CryptoManager
) {

    fun getAllServers(): Flow<List<Server>> = serverDao.getAll().map { entities ->
        entities.map { it.toServerWithSecrets() }
    }

    fun getGroups(): Flow<List<String?>> = serverDao.getGroups()

    suspend fun getServerById(id: Long): Server? {
        val entity = serverDao.getById(id) ?: return null
        return entity.toServerWithSecrets()
    }

    suspend fun saveServer(server: Server): Long {
        val entity = ServerEntity.fromServer(server, passwordEncrypted = null, privateKeyEncrypted = null)
        val id = if (server.id == 0L) {
            serverDao.insert(entity)
        } else {
            serverDao.update(entity)
            server.id
        }

        val passwordKey = CryptoManager.passwordKey(id)
        val privateKeyKey = CryptoManager.privateKeyKey(id)

        if (server.password.isNullOrEmpty()) {
            cryptoManager.removeSecure(passwordKey)
        } else {
            cryptoManager.saveSecure(passwordKey, server.password)
        }

        if (server.privateKey.isNullOrEmpty()) {
            cryptoManager.removeSecure(privateKeyKey)
        } else {
            cryptoManager.saveSecure(privateKeyKey, server.privateKey)
        }

        return id
    }

    suspend fun deleteServer(server: Server) {
        serverDao.deleteById(server.id)
        cryptoManager.removeSecure(CryptoManager.passwordKey(server.id))
        cryptoManager.removeSecure(CryptoManager.privateKeyKey(server.id))
    }

    private fun ServerEntity.toServerWithSecrets(): Server = toServer(
        password = cryptoManager.getSecure(CryptoManager.passwordKey(id)),
        privateKey = cryptoManager.getSecure(CryptoManager.privateKeyKey(id))
    )
}

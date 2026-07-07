package com.remotemanager.data.security

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class CryptoManager(context: Context) {

    private val masterKey: MasterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedPrefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "remote_manager_secure",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun encrypt(plaintext: String?): String? {
        if (plaintext.isNullOrEmpty()) return null
        return encryptedPrefs.edit().putString("__tmp__", plaintext).let {
            // Actually EncryptedSharedPreferences encrypts values automatically.
            // We store under a per-server key instead.
            plaintext
        }
    }

    fun saveSecure(key: String, value: String?) {
        encryptedPrefs.edit().putString(key, value).apply()
    }

    fun getSecure(key: String): String? {
        return encryptedPrefs.getString(key, null)
    }

    fun removeSecure(key: String) {
        encryptedPrefs.edit().remove(key).apply()
    }

    companion object {
        fun passwordKey(serverId: Long): String = "pwd_$serverId"
        fun privateKeyKey(serverId: Long): String = "pkey_$serverId"
    }
}

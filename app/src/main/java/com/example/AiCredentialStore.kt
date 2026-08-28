package com.example

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

interface AiCredentialStore {
    suspend fun getGeminiApiKey(): String?
    suspend fun setGeminiApiKey(key: String)
    suspend fun clearGeminiApiKey()
    suspend fun hasGeminiApiKey(): Boolean

    companion object {
        @Volatile
        private var instance: AiCredentialStore? = null

        fun getInstance(context: Context): AiCredentialStore {
            return instance ?: synchronized(this) {
                instance ?: KeystoreAiCredentialStore(context.applicationContext).also { instance = it }
            }
        }

        fun get(): AiCredentialStore? {
            return instance
        }
    }
}

class KeystoreAiCredentialStore(private val context: Context) : AiCredentialStore {

    private val alias = "AiGatewayGeminiKeyAlias"
    private val provider = "AndroidKeyStore"
    private val transformation = "AES/GCM/NoPadding"
    private val prefsName = "AiCredentialPrefs"
    private val keyEncryptedApiKey = "encrypted_gemini_api_key"
    private val keyIv = "iv_gemini_api_key"

    private val sharedPrefs by lazy {
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
    }

    init {
        try {
            // Ensure Key is created inside the Android Keystore
            val keyStore = KeyStore.getInstance(provider).apply { load(null) }
            if (!keyStore.containsAlias(alias)) {
                val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, provider)
                val spec = KeyGenParameterSpec.Builder(
                    alias,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build()
                keyGenerator.init(spec)
                keyGenerator.generateKey()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(provider).apply { load(null) }
        val entry = keyStore.getEntry(alias, null) as? KeyStore.SecretKeyEntry
        return entry?.secretKey ?: throw IllegalStateException("Failed to retrieve secret key from Android Keystore")
    }

    override suspend fun getGeminiApiKey(): String? {
        val encryptedBase64 = sharedPrefs.getString(keyEncryptedApiKey, null) ?: return null
        val ivBase64 = sharedPrefs.getString(keyIv, null) ?: return null

        return try {
            val encryptedBytes = Base64.decode(encryptedBase64, Base64.DEFAULT)
            val iv = Base64.decode(ivBase64, Base64.DEFAULT)

            val cipher = Cipher.getInstance(transformation)
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)

            val decryptedBytes = cipher.doFinal(encryptedBytes)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override suspend fun setGeminiApiKey(key: String) {
        try {
            val cipher = Cipher.getInstance(transformation)
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
            val encryptedBytes = cipher.doFinal(key.toByteArray(Charsets.UTF_8))
            val iv = cipher.iv

            sharedPrefs.edit()
                .putString(keyEncryptedApiKey, Base64.encodeToString(encryptedBytes, Base64.DEFAULT))
                .putString(keyIv, Base64.encodeToString(iv, Base64.DEFAULT))
                .apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun clearGeminiApiKey() {
        sharedPrefs.edit()
            .remove(keyEncryptedApiKey)
            .remove(keyIv)
            .apply()
    }

    override suspend fun hasGeminiApiKey(): Boolean {
        return getGeminiApiKey() != null
    }
}

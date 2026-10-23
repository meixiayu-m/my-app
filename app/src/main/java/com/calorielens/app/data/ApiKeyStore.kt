package com.calorielens.app.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** 个人模式下把智谱 Key 加密存到 Android Keystore，不写入 APK 或普通明文配置。 */
object ApiKeyStore {
    private const val KEY_ALIAS = "calorie_lens_zhipu_key"
    private const val PREFS = "private_settings"
    private const val VALUE = "zhipu_api_key"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"

    fun read(context: Context): String? = runCatching {
        val encoded = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(VALUE, null)
            ?: return null
        val bytes = Base64.decode(encoded, Base64.NO_WRAP)
        val iv = bytes.copyOfRange(0, 12)
        val encrypted = bytes.copyOfRange(12, bytes.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, iv))
        String(cipher.doFinal(encrypted), Charsets.UTF_8)
    }.getOrNull()

    fun save(context: Context, apiKey: String) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val encrypted = cipher.iv + cipher.doFinal(apiKey.trim().toByteArray(Charsets.UTF_8))
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(VALUE, Base64.encodeToString(encrypted, Base64.NO_WRAP))
            .apply()
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(VALUE).apply()
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existing != null) return existing
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }
}

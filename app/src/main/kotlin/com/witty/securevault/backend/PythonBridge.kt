package com.witty.securevault.backend

import android.content.Context
import android.util.Base64
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec

data class VaultCredential(
    val id: Long,
    val website: String,
    val websiteUrl: String = "",
    val username: String,
    val category: String,
    val isFavorite: Boolean,
    val passwordLength: Int = 0,
)

data class VaultCredentialDetail(
    val id: Long,
    val website: String,
    val websiteUrl: String = "",
    val username: String,
    val password: String,
    val category: String,
    val isFavorite: Boolean,
)

object PythonBridge {
    private const val iterations = 480_000
    private const val saltSize = 16
    private const val nonceSize = 12

    private var initialized = false
    private var masterKey: ByteArray? = null

    fun init(context: Context) {
        if (!Python.isStarted()) Python.start(AndroidPlatform(context))
        val response = call("init_db", File(context.filesDir, "secure_vault.db").absolutePath)
        require(response.optString("status") == "success") {
            response.optString("error", "Unable to initialize the vault database")
        }
        initialized = true
    }

    fun vaultExists(): Boolean = initialized && call("vault_exists").optBoolean("exists", false)

    fun createVault(password: String): Boolean {
        require(password.length >= 8) { "Key must be at least 8 characters" }
        val salt = ByteArray(saltSize).also(SecureRandom()::nextBytes)
        val key = deriveKey(password, salt)
        val response = call(
            "setup_vault",
            Base64.encodeToString(salt, Base64.NO_WRAP),
            verificationHash(key),
        )
        val success = response.optString("status") == "success"
        if (success) masterKey = key
        return success
    }

    fun unlock(password: String): Boolean {
        if (!initialized) return false
        val config = call("get_master_config")
        val data = config.optJSONObject("data") ?: return false
        return try {
            val key = deriveKey(password, Base64.decode(data.getString("salt"), Base64.NO_WRAP))
            val verified = constantTimeEquals(verificationHash(key), data.getString("verification_hash"))
            if (verified) masterKey = key
            verified
        } catch (_: Exception) {
            false
        }
    }

    fun lock() {
        masterKey?.fill(0)
        masterKey = null
    }

    fun isUnlocked(): Boolean = masterKey != null

    fun updateMasterPassword(newPassword: String): Boolean {
        require(newPassword.length >= 8) { "Key must be at least 8 characters" }
        val oldKey = masterKey ?: throw IllegalStateException("Vault is locked.")
        
        val newSalt = ByteArray(saltSize).also(SecureRandom()::nextBytes)
        val newKey = deriveKey(newPassword, newSalt)
        
        // Fetch all credentials
        val creds = credentials(false)
        val updates = JSONArray()
        
        for (c in creds) {
            val detail = credentialDetail(c.id, oldKey) ?: continue
            val newEncrypted = encrypt(detail.password, newKey)
            val obj = JSONObject()
            obj.put("id", c.id)
            obj.put("secret", newEncrypted)
            updates.put(obj)
        }
        
        val response = call(
            "update_master_password_data",
            Base64.encodeToString(newSalt, Base64.NO_WRAP),
            verificationHash(newKey),
            updates.toString()
        )
        
        val success = response.optString("status") == "success"
        if (success) {
            masterKey = newKey
        } else {
            throw IllegalArgumentException(response.optString("error", "Failed to update password"))
        }
        return success
    }

    fun credentials(favoritesOnly: Boolean = false): List<VaultCredential> {
        val key = masterKey ?: return emptyList()
        val response = call("list_credentials", favoritesOnly)
        val data = response.optJSONArray("data") ?: return emptyList()
        return buildList {
            for (index in 0 until data.length()) {
                val item = data.getJSONObject(index)
                val passwordLength = credentialDetail(item.getLong("id"), key)?.password?.length ?: 0
                add(item.toCredential(passwordLength))
            }
        }
    }

    fun credentialDetail(id: Long): VaultCredentialDetail? = masterKey?.let { credentialDetail(id, it) }

    fun saveCredential(id: Long?, website: String, websiteUrl: String, username: String, password: String, category: String, autoFavorite: Boolean = false): Boolean {
        require(website.isNotBlank()) { "Service is required" }
        require(password.isNotBlank()) { "Password is required" }
        val currentKey = masterKey ?: throw IllegalStateException("Vault is locked. Master key is missing.")
        val encrypted = encrypt(password, currentKey)
        val response = if (id == null) {
            call("add_credential", website.trim(), username.trim(), encrypted, category.trim(), websiteUrl.trim())
        } else {
            call("update_credential", id, website.trim(), username.trim(), encrypted, category.trim(), websiteUrl.trim())
        }
        val isSuccess = response.optString("status") == "success"
        if (!isSuccess) {
            throw IllegalArgumentException(response.optString("error", "Database error"))
        }
        
        if (id == null && autoFavorite) {
            val newId = response.optLong("id", -1)
            if (newId != -1L) {
                setFavorite(newId, true)
            }
        }
        return true
    }

    fun deleteCredential(id: Long): Boolean =
        isUnlocked() && call("delete_credential", id).optString("status") == "success"

    fun setFavorite(id: Long, favorite: Boolean): Boolean =
        isUnlocked() && call("toggle_favorite", id, favorite).optString("status") == "success"

    fun categories(): Map<String, Int> {
        val data = call("category_summary").optJSONObject("data") ?: return emptyMap()
        return data.keys().asSequence().associateWith { data.optInt(it) }
    }

    fun generatePassword(length: Int = 16): String {
        val alphabet = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!@#$%^&*()_+~`|}{[]:;?><,./-="
        val random = SecureRandom()
        return buildString(length) { repeat(length) { append(alphabet[random.nextInt(alphabet.length)]) } }
    }

    private fun credentialDetail(id: Long, key: ByteArray): VaultCredentialDetail? {
        val data = call("get_credential", id).optJSONObject("data") ?: return null
        return try {
            VaultCredentialDetail(
                id = data.getLong("id"),
                website = data.getString("website"),
                websiteUrl = data.optString("website_url", ""),
                username = data.getString("username"),
                password = decrypt(data.getString("encrypted_secret"), key),
                category = data.optString("category"),
                isFavorite = data.optInt("is_favorite") == 1,
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun JSONObject.toCredential(passwordLength: Int) = VaultCredential(
        id = getLong("id"),
        website = getString("website"),
        websiteUrl = optString("website_url", ""),
        username = optString("username"),
        category = optString("category"),
        isFavorite = optInt("is_favorite") == 1,
        passwordLength = passwordLength,
    )

    private fun call(attribute: String, vararg args: Any): JSONObject {
        check(initialized || attribute == "init_db") { "Vault engine is not initialized" }
        val result = Python.getInstance().getModule("vault_backend").callAttr(attribute, *args).toString()
        return JSONObject(result)
    }

    private fun deriveKey(password: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, iterations, 256)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
    }

    private fun verificationHash(key: ByteArray): String = Base64.encodeToString(
        MessageDigest.getInstance("SHA-256").digest(key), Base64.NO_WRAP,
    )

    private fun constantTimeEquals(left: String, right: String): Boolean =
        MessageDigest.isEqual(left.toByteArray(), right.toByteArray())

    private fun encrypt(plainText: String, key: ByteArray): String {
        val nonce = ByteArray(nonceSize).also(SecureRandom()::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, javax.crypto.spec.SecretKeySpec(key, "AES"), GCMParameterSpec(128, nonce))
        return Base64.encodeToString(nonce + cipher.doFinal(plainText.toByteArray()), Base64.NO_WRAP)
    }

    private fun decrypt(cipherText: String, key: ByteArray): String {
        val combined = Base64.decode(cipherText, Base64.NO_WRAP)
        require(combined.size > nonceSize) { "Invalid encrypted credential" }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, javax.crypto.spec.SecretKeySpec(key, "AES"), GCMParameterSpec(128, combined.copyOfRange(0, nonceSize)))
        return cipher.doFinal(combined.copyOfRange(nonceSize, combined.size)).decodeToString()
    }
}

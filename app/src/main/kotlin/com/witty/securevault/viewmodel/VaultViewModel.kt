package com.witty.securevault.viewmodel

import androidx.lifecycle.ViewModel
import com.witty.securevault.backend.PythonBridge
import com.witty.securevault.backend.VaultCredential
import com.witty.securevault.backend.VaultCredentialDetail
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppState { STARTUP, SETUP, LOGIN, DASHBOARD }
enum class VaultSection { ALL, FAVORITES, WEAK, MORE }

class VaultViewModel : ViewModel() {
    private val _appState = MutableStateFlow(AppState.STARTUP)
    val appState: StateFlow<AppState> = _appState.asStateFlow()

    private val _credentials = MutableStateFlow<List<VaultCredential>>(emptyList())
    val credentials: StateFlow<List<VaultCredential>> = _credentials.asStateFlow()

    private val _section = MutableStateFlow(VaultSection.ALL)
    val section: StateFlow<VaultSection> = _section.asStateFlow()

    private val _errorMessage = MutableStateFlow("")
    val errorMessage: StateFlow<String> = _errorMessage.asStateFlow()

    private val _categories = MutableStateFlow<Map<String, Int>>(emptyMap())
    val categories: StateFlow<Map<String, Int>> = _categories.asStateFlow()

    fun loadInitialState() {
        _appState.value = if (PythonBridge.vaultExists()) AppState.LOGIN else AppState.SETUP
    }

    fun setupVault(password: String, confirmation: String) {
        when {
            password.length < 8 -> showError("Key must be at least 8 characters")
            password != confirmation -> showError("Keys do not match")
            PythonBridge.createVault(password) -> openDashboard()
            else -> showError("Unable to create the vault")
        }
    }

    fun attemptLogin(password: String) {
        if (password.isBlank()) return showError("Incorrect password")
        if (PythonBridge.unlock(password)) openDashboard() else showError("Incorrect password")
    }

    fun lockVault() {
        PythonBridge.lock()
        _credentials.value = emptyList()
        _section.value = VaultSection.ALL
        _appState.value = AppState.LOGIN
    }

    fun selectSection(section: VaultSection) {
        _section.value = section
        refreshCredentials()
    }

        private val categoryRules = mapOf(
        "Social" to listOf("facebook", "instagram", "twitter", "x.com", "tiktok", "snapchat", "reddit", "linkedin", "pinterest", "discord", "whatsapp", "telegram"),
        "Finance" to listOf("bank", "hdfc", "icici", "sbi", "chase", "paypal", "stripe", "gpay", "paytm", "phonepe", "wise", "crypto", "binance", "coinbase", "wallet", "groww", "zerodha"),
        "Work" to listOf("github", "gitlab", "bitbucket", "jira", "slack", "notion", "aws", "azure", "gcp", "trello", "teams", "zoom", "vercel"),
        "Shopping" to listOf("amazon", "flipkart", "ebay", "walmart", "shopify", "myntra", "ajio", "zomato", "swiggy", "ubereats", "blinkit", "meesho"),
        "Email" to listOf("gmail", "outlook", "hotmail", "yahoo", "proton", "zoho", "mail", "icloud"),
        "Entertainment" to listOf("netflix", "spotify", "youtube", "hulu", "prime video", "disney", "twitch", "steam", "epic", "playstation", "xbox", "hotstar", "zee5", "sonyliv"),
        "Education" to listOf("coursera", "udemy", "edx", "skillshare", "leetcode", "hackerrank", "duolingo", "byjus", "unacademy")
    )

    private fun autoCategorize(website: String): String {
        val lowerWeb = website.lowercase().trim()
        for ((cat, keywords) in categoryRules) {
            for (keyword in keywords) {
                if (lowerWeb.contains(keyword)) return cat
            }
        }
        return "Other"
    }

    fun saveCredential(id: Long?, website: String, websiteUrl: String, username: String, password: String, category: String, autoFavorite: Boolean = false): Boolean {
        val finalCategory = if (category.isBlank()) autoCategorize(website) else category
        val success = try {
            PythonBridge.saveCredential(id, website, websiteUrl, username, password, finalCategory, autoFavorite)
        } catch (error: Exception) {
            android.util.Log.e("SERET", "Error saving credential", error)
            showError(error.message ?: "Unable to save credential")
            false
        }
        if (success) refreshCredentials()
        return success
    }

    fun credentialDetail(id: Long): VaultCredentialDetail? = PythonBridge.credentialDetail(id)

    fun updateMasterPassword(newPassword: String, confirmation: String): Boolean {
        if (newPassword.length < 8) { showError("Key must be at least 8 characters"); return false }
        if (newPassword != confirmation) { showError("Keys do not match"); return false }
        return try {
            val success = PythonBridge.updateMasterPassword(newPassword)
            if (success) {
                _errorMessage.value = "Password updated successfully!"
            }
            success
        } catch (e: Exception) {
            showError(e.message ?: "Failed to update")
            false
        }
    }

    fun deleteCredential(id: Long) {
        if (PythonBridge.deleteCredential(id)) refreshCredentials()
    }

    fun toggleFavorite(id: Long, favorite: Boolean) {
        if (PythonBridge.setFavorite(id, favorite)) refreshCredentials()
    }

    fun generatePassword(): String = PythonBridge.generatePassword()

    fun resetError() { _errorMessage.value = "" }

    private fun openDashboard() {
        _errorMessage.value = ""
        _appState.value = AppState.DASHBOARD
        refreshCredentials()
    }

    private fun refreshCredentials() {
        val favoritesOnly = _section.value == VaultSection.FAVORITES
        _credentials.value = PythonBridge.credentials(favoritesOnly)
        _categories.value = PythonBridge.categories()
    }

    private fun showError(message: String) { _errorMessage.value = message }
}

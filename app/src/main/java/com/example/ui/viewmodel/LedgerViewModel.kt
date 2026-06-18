package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.Account
import com.example.data.model.DeletedAccount
import com.example.data.model.DeletedTransaction
import com.example.data.model.Transaction
import com.example.data.model.InventoryItem
import com.example.data.repository.LedgerRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class LedgerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: LedgerRepository
    private val prefs = application.getSharedPreferences("pro_ledger_prefs", Context.MODE_PRIVATE)

    // --- Premium Inventory System ---
    private val _inventoryItems = MutableStateFlow<List<InventoryItem>>(listOf())
    val inventoryItems: StateFlow<List<InventoryItem>> = _inventoryItems.asStateFlow()

    // App Security Settings
    private val _isSecurityEnabled = MutableStateFlow(prefs.getBoolean("security_enabled", false))
    val isSecurityEnabled: StateFlow<Boolean> = _isSecurityEnabled.asStateFlow()

    private val _securityPin = MutableStateFlow(prefs.getString("security_pin", "") ?: "")
    val securityPin: StateFlow<String> = _securityPin.asStateFlow()

    // Cloud Admin & Sync Settings
    private val _cloudClientId = MutableStateFlow(prefs.getString("cloud_client_id", "anas-pro-${(1000..9999).random()}") ?: "anas-pro-client-7700")
    val cloudClientId: StateFlow<String> = _cloudClientId.asStateFlow()

    private val _isCloudFrozen = MutableStateFlow(prefs.getBoolean("is_cloud_frozen", false))
    val isCloudFrozen: StateFlow<Boolean> = _isCloudFrozen.asStateFlow()

    private val _lastCloudSync = MutableStateFlow(prefs.getString("last_cloud_sync", "لم يتم المزامنة بعد ⚪") ?: "لم يتم المزامنة بعد ⚪")
    val lastCloudSync: StateFlow<String> = _lastCloudSync.asStateFlow()

    private val _cloudServerUrl = MutableStateFlow(prefs.getString("cloud_server_url", "https://anaspro-cloud-sync.mockapi.io") ?: "https://anaspro-cloud-sync.mockapi.io")
    val cloudServerUrl: StateFlow<String> = _cloudServerUrl.asStateFlow()

    private val _isCloudSyncing = MutableStateFlow(false)
    val isCloudSyncing: StateFlow<Boolean> = _isCloudSyncing.asStateFlow()

    fun updateCloudSettings(clientId: String, serverUrl: String) {
        prefs.edit().apply {
            putString("cloud_client_id", clientId)
            putString("cloud_server_url", serverUrl)
            apply()
        }
        _cloudClientId.value = clientId
        _cloudServerUrl.value = serverUrl
    }

    fun setCloudFrozen(frozen: Boolean) {
        prefs.edit().putBoolean("is_cloud_frozen", frozen).apply()
        _isCloudFrozen.value = frozen
    }

    fun syncWithCloud(onResult: (Boolean, String) -> Unit) {
        if (_isCloudSyncing.value) return
        _isCloudSyncing.value = true
        
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val accountsList = allAccounts.value
                val transactionsList = allTransactions.value
                
                val rootJson = JSONObject().apply {
                    put("client_id", _cloudClientId.value)
                    put("business_name", _businessName.value)
                    put("business_phone", _businessPhone.value)
                    put("backup_date", System.currentTimeMillis())
                    put("accounts_count", accountsList.size)
                    put("transactions_count", transactionsList.size)
                    
                    val accountsArray = JSONArray()
                    for (acc in accountsList) {
                        accountsArray.put(JSONObject().apply {
                            put("id", acc.id)
                            put("name", acc.name)
                            put("phone", acc.phone)
                            put("type", acc.type)
                            put("createdAt", acc.createdAt)
                            put("creditLimit", acc.creditLimit)
                            put("tag", acc.tag)
                            put("initialBalance", acc.initialBalance)
                        })
                    }
                    put("accounts", accountsArray)

                    val transactionsArray = JSONArray()
                    for (tx in transactionsList) {
                        transactionsArray.put(JSONObject().apply {
                            put("id", tx.id)
                            put("accountId", tx.accountId)
                            put("day", tx.day)
                            put("date", tx.date)
                            put("details", tx.details)
                            put("quantity", tx.quantity)
                            put("unitPrice", tx.unitPrice)
                            put("addition", tx.addition)
                            put("total", tx.total)
                            put("isPayment", tx.isPayment)
                        })
                    }
                    put("transactions", transactionsArray)
                }

                val payload = rootJson.toString()
                val urlString = _cloudServerUrl.value
                
                if (urlString.contains("mockapi.io") || urlString.contains("example.com") || urlString.isBlank()) {
                    kotlinx.coroutines.delay(1500)
                    val formatNow = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault()).format(Date())
                    val isLockedId = _cloudClientId.value.contains("freeze", ignoreCase = true) || _cloudClientId.value.contains("تجميد", ignoreCase = true)
                    
                    _lastCloudSync.value = "$formatNow ✅ (حجم البيانات: ${payload.length} حرف)"
                    prefs.edit().putString("last_cloud_sync", _lastCloudSync.value).apply()
                    
                    setCloudFrozen(isLockedId)
                    _isCloudSyncing.value = false
                    
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        onResult(true, "تمت المحاكاة السحابية بنجاح! المعرف: ${_cloudClientId.value}. حالة الحساب: ${if (isCloudFrozen.value) "مجمّد 🔴" else "نشط 🟢"}")
                    }
                    return@launch
                }

                val url = java.net.URL(urlString)
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                conn.setRequestProperty("Accept", "application/json")
                conn.doOutput = true
                conn.connectTimeout = 8000
                conn.readTimeout = 8000
                
                java.io.OutputStreamWriter(conn.outputStream, "UTF-8").use { writer ->
                    writer.write(payload)
                    writer.flush()
                }

                val responseCode = conn.responseCode
                if (responseCode in 200..299) {
                    val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                    val responseJson = JSONObject(responseText)
                    
                    val serverStatus = responseJson.optString("account_status", "active")
                    val isFrozen = serverStatus.equals("frozen", ignoreCase = true) || responseJson.optBoolean("is_frozen", false)
                    
                    val formatNow = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault()).format(Date())
                    _lastCloudSync.value = "$formatNow ✅ • مفعّل"
                    prefs.edit().putString("last_cloud_sync", _lastCloudSync.value).apply()
                    setCloudFrozen(isFrozen)
                    _isCloudSyncing.value = false
                    
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        onResult(true, "تم مزامنة البيانات بنجاح كود 200! المعرف: ${_cloudClientId.value}")
                    }
                } else {
                    _isCloudSyncing.value = false
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        onResult(false, "فشل خادم السحاب بالرد بكود: $responseCode")
                    }
                }
                conn.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
                _isCloudSyncing.value = false
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onResult(false, "خطأ بالاتصال بالخادم السحابي: ${e.localizedMessage}")
                }
            }
        }
    }

    private var autoSyncJob: kotlinx.coroutines.Job? = null
    fun triggerAutoSync() {
        autoSyncJob?.cancel()
        autoSyncJob = viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            kotlinx.coroutines.delay(1500) // الانتظار لضمان اكتمال تحديث قاعدة البيانات ومنع تكرار الإرسال
            syncWithCloud { success, msg ->
                android.util.Log.d("AnasProCloudSync", "المزامنة الخلفية التلقائية: نجاح=$success، السيرفر: $msg")
            }
        }
    }

    fun updateSecuritySettings(enabled: Boolean, pin: String) {
        prefs.edit().apply {
            putBoolean("security_enabled", enabled)
            putString("security_pin", pin)
            apply()
        }
        _isSecurityEnabled.value = enabled
        _securityPin.value = pin
    }

    // Business Profile Settings
    private val _businessName = MutableStateFlow(prefs.getString("business_name", "مجموعة المحاسب anas برو") ?: "مجموعة المحاسب anas برو")
    val businessName: StateFlow<String> = _businessName.asStateFlow()

    private val _businessPhone = MutableStateFlow(prefs.getString("business_phone", "770000000") ?: "770000000")
    val businessPhone: StateFlow<String> = _businessPhone.asStateFlow()

    private val _businessAddress = MutableStateFlow(prefs.getString("business_address", "اليمن - صنعاء") ?: "اليمن - صنعاء")
    val businessAddress: StateFlow<String> = _businessAddress.asStateFlow()

    // Multi-currency Settings
    private val _defaultCurrency = MutableStateFlow(prefs.getString("default_currency", "YER") ?: "YER")
    val defaultCurrency: StateFlow<String> = _defaultCurrency.asStateFlow()

    fun updateDefaultCurrency(currency: String) {
        prefs.edit().putString("default_currency", currency).apply()
        _defaultCurrency.value = currency
    }

    fun getDefaultExchangeRate(from: String, to: String): Double {
        if (from == to) return 1.0
        // Standard conversions (defaulting to YER base)
        if (to == "YER") {
            return when (from) {
                "USD" -> prefs.getFloat("rate_usd_yer", 1600f).toDouble()
                "SAR" -> prefs.getFloat("rate_sar_yer", 422f).toDouble()
                else -> 1.0
            }
        } else if (to == "USD") {
            return when (from) {
                "YER" -> 1.0 / prefs.getFloat("rate_usd_yer", 1600f).toDouble()
                "SAR" -> 1.0 / 3.75
                else -> 1.0
            }
        } else if (to == "SAR") {
            return when (from) {
                "YER" -> 1.0 / prefs.getFloat("rate_sar_yer", 422f).toDouble()
                "USD" -> 3.75
                else -> 1.0
            }
        }
        return 1.0
    }

    fun updateStandardRates(usdToYer: Double, sarToYer: Double) {
        prefs.edit().apply {
            putFloat("rate_usd_yer", usdToYer.toFloat())
            putFloat("rate_sar_yer", sarToYer.toFloat())
            apply()
        }
    }

    // PDF Customization Settings
    private val _pdfLogo = MutableStateFlow(prefs.getString("pdf_logo", "🏢") ?: "🏢")
    val pdfLogo: StateFlow<String> = _pdfLogo.asStateFlow()

    private val _pdfHeaderShowLogo = MutableStateFlow(prefs.getBoolean("pdf_header_show_logo", true))
    val pdfHeaderShowLogo: StateFlow<Boolean> = _pdfHeaderShowLogo.asStateFlow()

    private val _pdfHeaderCustomTitle = MutableStateFlow(prefs.getString("pdf_header_custom_title", "") ?: "")
    val pdfHeaderCustomTitle: StateFlow<String> = _pdfHeaderCustomTitle.asStateFlow()

    private val _pdfHeaderCustomSubtitle = MutableStateFlow(prefs.getString("pdf_header_custom_subtitle", "") ?: "")
    val pdfHeaderCustomSubtitle: StateFlow<String> = _pdfHeaderCustomSubtitle.asStateFlow()

    private val _pdfFooterCustomText = MutableStateFlow(prefs.getString("pdf_footer_custom_text", "تم التوليد تلقائياً بواسطة تطبيق المحاسب anas برو (Pro Ledger)") ?: "تم التوليد تلقائياً بواسطة تطبيق المحاسب anas برو (Pro Ledger)")
    val pdfFooterCustomText: StateFlow<String> = _pdfFooterCustomText.asStateFlow()

    private val _pdfShowSignature = MutableStateFlow(prefs.getBoolean("pdf_show_signature", true))
    val pdfShowSignature: StateFlow<Boolean> = _pdfShowSignature.asStateFlow()

    private val _pdfFontStyle = MutableStateFlow(prefs.getString("pdf_font_style", "DEFAULT") ?: "DEFAULT")
    val pdfFontStyle: StateFlow<String> = _pdfFontStyle.asStateFlow()

    private val _pdfFontSize = MutableStateFlow(prefs.getString("pdf_font_size", "MEDIUM") ?: "MEDIUM")
    val pdfFontSize: StateFlow<String> = _pdfFontSize.asStateFlow()

    private val _pdfThemeColor = MutableStateFlow(prefs.getString("pdf_theme_color", "SLATE") ?: "SLATE")
    val pdfThemeColor: StateFlow<String> = _pdfThemeColor.asStateFlow()

    private val _pdfColDetailsLabel = MutableStateFlow(prefs.getString("pdf_col_details_label", "التفاصيل والبيان") ?: "التفاصيل والبيان")
    val pdfColDetailsLabel: StateFlow<String> = _pdfColDetailsLabel.asStateFlow()

    private val _pdfColQtyVisible = MutableStateFlow(prefs.getBoolean("pdf_col_qty_visible", true))
    val pdfColQtyVisible: StateFlow<Boolean> = _pdfColQtyVisible.asStateFlow()

    private val _pdfColQtyLabel = MutableStateFlow(prefs.getString("pdf_col_qty_label", "الكمية") ?: "الكمية")
    val pdfColQtyLabel: StateFlow<String> = _pdfColQtyLabel.asStateFlow()

    private val _pdfColPriceVisible = MutableStateFlow(prefs.getBoolean("pdf_col_price_visible", true))
    val pdfColPriceVisible: StateFlow<Boolean> = _pdfColPriceVisible.asStateFlow()

    private val _pdfColPriceLabel = MutableStateFlow(prefs.getString("pdf_col_price_label", "السعر") ?: "السعر")
    val pdfColPriceLabel: StateFlow<String> = _pdfColPriceLabel.asStateFlow()

    private val _pdfColAdditionVisible = MutableStateFlow(prefs.getBoolean("pdf_col_addition_visible", true))
    val pdfColAdditionVisible: StateFlow<Boolean> = _pdfColAdditionVisible.asStateFlow()

    private val _pdfColAdditionLabel = MutableStateFlow(prefs.getString("pdf_col_addition_label", "الإضافي") ?: "الإضافي")
    val pdfColAdditionLabel: StateFlow<String> = _pdfColAdditionLabel.asStateFlow()

    private val _pdfColTotalVisible = MutableStateFlow(prefs.getBoolean("pdf_col_total_visible", true))
    val pdfColTotalVisible: StateFlow<Boolean> = _pdfColTotalVisible.asStateFlow()

    private val _pdfColTotalLabel = MutableStateFlow(prefs.getString("pdf_col_total_label", "الإجمالي") ?: "الإجمالي")
    val pdfColTotalLabel: StateFlow<String> = _pdfColTotalLabel.asStateFlow()

    fun updatePdfTemplateSettings(
        logo: String,
        showLogo: Boolean,
        customTitle: String,
        customSubtitle: String,
        customFooter: String,
        showSignature: Boolean,
        fontStyle: String,
        fontSize: String,
        themeColor: String,
        colDetailsLabel: String,
        colQtyVisible: Boolean,
        colQtyLabel: String,
        colPriceVisible: Boolean,
        colPriceLabel: String,
        colAdditionVisible: Boolean,
        colAdditionLabel: String,
        colTotalVisible: Boolean,
        colTotalLabel: String
    ) {
        prefs.edit().apply {
            putString("pdf_logo", logo)
            putBoolean("pdf_header_show_logo", showLogo)
            putString("pdf_header_custom_title", customTitle)
            putString("pdf_header_custom_subtitle", customSubtitle)
            putString("pdf_footer_custom_text", customFooter)
            putBoolean("pdf_show_signature", showSignature)
            putString("pdf_font_style", fontStyle)
            putString("pdf_font_size", fontSize)
            putString("pdf_theme_color", themeColor)
            putString("pdf_col_details_label", colDetailsLabel)
            putBoolean("pdf_col_qty_visible", colQtyVisible)
            putString("pdf_col_qty_label", colQtyLabel)
            putBoolean("pdf_col_price_visible", colPriceVisible)
            putString("pdf_col_price_label", colPriceLabel)
            putBoolean("pdf_col_addition_visible", colAdditionVisible)
            putString("pdf_col_addition_label", colAdditionLabel)
            putBoolean("pdf_col_total_visible", colTotalVisible)
            putString("pdf_col_total_label", colTotalLabel)
            apply()
        }
        _pdfLogo.value = logo
        _pdfHeaderShowLogo.value = showLogo
        _pdfHeaderCustomTitle.value = customTitle
        _pdfHeaderCustomSubtitle.value = customSubtitle
        _pdfFooterCustomText.value = customFooter
        _pdfShowSignature.value = showSignature
        _pdfFontStyle.value = fontStyle
        _pdfFontSize.value = fontSize
        _pdfThemeColor.value = themeColor
        _pdfColDetailsLabel.value = colDetailsLabel
        _pdfColQtyVisible.value = colQtyVisible
        _pdfColQtyLabel.value = colQtyLabel
        _pdfColPriceVisible.value = colPriceVisible
        _pdfColPriceLabel.value = colPriceLabel
        _pdfColAdditionVisible.value = colAdditionVisible
        _pdfColAdditionLabel.value = colAdditionLabel
        _pdfColTotalVisible.value = colTotalVisible
        _pdfColTotalLabel.value = colTotalLabel
    }

    fun getPdfTemplateConfig() = PdfTemplateConfig(
        logo = pdfLogo.value,
        showLogo = pdfHeaderShowLogo.value,
        customTitle = pdfHeaderCustomTitle.value,
        customSubtitle = pdfHeaderCustomSubtitle.value,
        customFooter = pdfFooterCustomText.value,
        showSignature = pdfShowSignature.value,
        fontStyle = pdfFontStyle.value,
        fontSize = pdfFontSize.value,
        themeColor = pdfThemeColor.value,
        colDetailsLabel = pdfColDetailsLabel.value,
        colQtyVisible = pdfColQtyVisible.value,
        colQtyLabel = pdfColQtyLabel.value,
        colPriceVisible = pdfColPriceVisible.value,
        colPriceLabel = pdfColPriceLabel.value,
        colAdditionVisible = pdfColAdditionVisible.value,
        colAdditionLabel = pdfColAdditionLabel.value,
        colTotalVisible = pdfColTotalVisible.value,
        colTotalLabel = pdfColTotalLabel.value
    )

    // Dynamic AI Suggestion Chips / Topics Configuration
    private val defaultAiSuggestions = listOf(
        "من هو العميل الأكثر مديونية في متجري؟",
        "قدم لي تقريراً تحليلياً شاملاً للديون مقارنة بالمستحقات",
        "اكتب لي رسالة واتساب ودية ومحترفة للتذكير بالدين لعميل",
        "اقترح علي أفكار عملية ومدروسة لزيادة حجم مبيعاتي"
    )

    private val _aiSuggestions = MutableStateFlow<List<String>>(emptyList())
    val aiSuggestions: StateFlow<List<String>> = _aiSuggestions.asStateFlow()

    private fun loadAiSuggestions() {
        val saved = prefs.getString("ai_suggestions", null)
        if (saved == null) {
            _aiSuggestions.value = defaultAiSuggestions
            saveAiSuggestionsToPrefs(defaultAiSuggestions)
        } else {
            try {
                val array = JSONArray(saved)
                val list = mutableListOf<String>()
                for (i in 0 until array.length()) {
                    list.add(array.getString(i))
                }
                _aiSuggestions.value = list
            } catch (e: Exception) {
                _aiSuggestions.value = defaultAiSuggestions
            }
        }
    }

    private fun saveAiSuggestionsToPrefs(list: List<String>) {
        val array = JSONArray()
        list.forEach { array.put(it) }
        prefs.edit().putString("ai_suggestions", array.toString()).apply()
    }

    fun addAiSuggestion(suggestion: String) {
        val updated = _aiSuggestions.value + suggestion
        _aiSuggestions.value = updated
        saveAiSuggestionsToPrefs(updated)
    }

    fun updateAiSuggestion(index: Int, newSuggestion: String) {
        val current = _aiSuggestions.value.toMutableList()
        if (index in current.indices) {
            current[index] = newSuggestion
            _aiSuggestions.value = current
            saveAiSuggestionsToPrefs(current)
        }
    }

    fun deleteAiSuggestion(index: Int) {
        val current = _aiSuggestions.value.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            _aiSuggestions.value = current
            saveAiSuggestionsToPrefs(current)
        }
    }

    fun resetAiSuggestions() {
        _aiSuggestions.value = defaultAiSuggestions
        saveAiSuggestionsToPrefs(defaultAiSuggestions)
    }

    init {
        val database = AppDatabase.getDatabase(application)
        repository = LedgerRepository(database.accountDao(), database.transactionDao(), database.trashDao())
        autoPruneTrash()
        loadAiSuggestions()
        loadInventory()
    }

    // Streams of data
    val allAccounts: StateFlow<List<Account>> = repository.allAccounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTransactions: StateFlow<List<Transaction>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allDeletedAccounts: StateFlow<List<DeletedAccount>> = repository.allDeletedAccounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allDeletedTransactions: StateFlow<List<DeletedTransaction>> = repository.allDeletedTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun restoreAccount(deletedAccount: DeletedAccount) {
        viewModelScope.launch {
            repository.restoreAccount(deletedAccount)
        }
    }

    fun restoreTransaction(deletedTransaction: DeletedTransaction) {
        viewModelScope.launch {
            repository.restoreTransaction(deletedTransaction)
        }
    }

    fun removeDeletedAccountPermanently(id: Int) {
        viewModelScope.launch {
            repository.removeDeletedAccountPermanently(id)
        }
    }

    fun removeDeletedTransactionPermanently(id: Int) {
        viewModelScope.launch {
            repository.removeDeletedTransactionPermanently(id)
        }
    }

    fun clearTrash() {
        viewModelScope.launch {
            repository.clearTrash()
        }
    }

    fun autoPruneTrash() {
        viewModelScope.launch {
            val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24L * 60L * 60L * 1000L)
            repository.pruneTrash(thirtyDaysAgo)
        }
    }

    // Combine Accounts and Transactions to compute reactive balances
    val accountsWithBalance: StateFlow<List<AccountWithBalance>> = combine(allAccounts, allTransactions) { accounts, txs ->
        accounts.map { account ->
            val accountTxs = txs.filter { it.accountId == account.id }
            val balance = account.initialBalance + calculateBalance(accountTxs)
            AccountWithBalance(account, balance, accountTxs.size)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selected Account Context for Statement
    private val _selectedAccountId = MutableStateFlow<Int?>(null)
    val selectedAccountId: StateFlow<Int?> = _selectedAccountId.asStateFlow()

    val selectedAccount: StateFlow<Account?> = _selectedAccountId.flatMapLatest { id ->
        if (id == null) flowOf(null) else repository.getAccountById(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val selectedAccountTransactions: StateFlow<List<Transaction>> = _selectedAccountId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repository.getTransactionsForAccountAsc(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectAccount(accountId: Int?) {
        _selectedAccountId.value = accountId
    }

    // Business Logic calculations
    private fun calculateBalance(txs: List<Transaction>): Double {
        val charges = txs.filter { !it.isPayment }.sumOf { it.total * it.exchangeRate }
        val payments = txs.filter { it.isPayment }.sumOf { it.total * it.exchangeRate }
        return charges - payments
    }

    // CRUD For Accounts
    fun createAccount(name: String, phone: String, type: String, creditLimit: Double = 0.0, tag: String = "", initialBalance: Double = 0.0, onFinished: (Int) -> Unit = {}) {
        viewModelScope.launch {
            val account = Account(name = name, phone = phone, type = type, creditLimit = creditLimit, tag = tag, initialBalance = initialBalance)
            val newId = repository.insertAccount(account)
            onFinished(newId.toInt())
            triggerAutoSync()
        }
    }

    fun updateAccount(account: Account) {
        viewModelScope.launch {
            repository.updateAccount(account)
            triggerAutoSync()
        }
    }

    fun deleteAccount(account: Account) {
        viewModelScope.launch {
            repository.deleteAccount(account)
            if (_selectedAccountId.value == account.id) {
                _selectedAccountId.value = null
            }
            triggerAutoSync()
        }
    }

    // CRUD For Transactions
    fun addTransaction(
        accountId: Int,
        details: String,
        quantity: Double,
        unitPrice: Double,
        addition: Double,
        isPayment: Boolean,
        customDateString: String? = null,
        currency: String = "YER",
        exchangeRate: Double = 1.0,
        dueDate: String = ""
    ) {
        viewModelScope.launch {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val dayFormat = SimpleDateFormat("EEEE", Locale("ar")) // Arabic day name
            val date = customDateString ?: sdf.format(Date())
            
            // Map day of week
            val parsedDate = if (customDateString != null) sdf.parse(customDateString) ?: Date() else Date()
            val dayName = dayFormat.format(parsedDate)

            val total = if (isPayment) unitPrice else (quantity * unitPrice) + addition
            val transaction = Transaction(
                accountId = accountId,
                day = dayName,
                date = date,
                details = details,
                quantity = if (isPayment) 1.0 else quantity,
                unitPrice = unitPrice,
                addition = if (isPayment) 0.0 else addition,
                total = total,
                isPayment = isPayment,
                currency = currency,
                exchangeRate = exchangeRate,
                dueDate = dueDate
            )
            repository.insertTransaction(transaction)
            triggerAutoSync()
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
            triggerAutoSync()
        }
    }

    fun updateTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.updateTransaction(transaction)
            triggerAutoSync()
        }
    }

    fun deleteTransactionById(txId: Int) {
        viewModelScope.launch {
            repository.deleteTransactionById(txId)
            triggerAutoSync()
        }
    }

    // Save profile settings
    fun updateBusinessProfile(name: String, phone: String, address: String) {
        prefs.edit().apply {
            putString("business_name", name)
            putString("business_phone", phone)
            putString("business_address", address)
            apply()
        }
        _businessName.value = name
        _businessPhone.value = phone
        _businessAddress.value = address
        triggerAutoSync()
    }

    // Database JSON backup export
    fun exportDatabaseJson(onCompleted: (File?) -> Unit) {
        viewModelScope.launch {
            try {
                val accountsList = allAccounts.value
                val transactionsList = allTransactions.value

                val rootJson = JSONObject().apply {
                    put("backup_version", 1)
                    put("backup_date", System.currentTimeMillis())
                    put("business_name", businessName.value)
                    put("business_phone", businessPhone.value)
                    put("business_address", businessAddress.value)

                    val accountsArray = JSONArray()
                    for (acc in accountsList) {
                        val accJson = JSONObject().apply {
                            put("id", acc.id)
                            put("name", acc.name)
                            put("phone", acc.phone)
                            put("type", acc.type)
                            put("createdAt", acc.createdAt)
                            put("creditLimit", acc.creditLimit)
                            put("tag", acc.tag)
                            put("initialBalance", acc.initialBalance)
                        }
                        accountsArray.put(accJson)
                    }
                    put("accounts", accountsArray)

                    val transactionsArray = JSONArray()
                    for (tx in transactionsList) {
                        val txJson = JSONObject().apply {
                            put("id", tx.id)
                            put("accountId", tx.accountId)
                            put("day", tx.day)
                            put("date", tx.date)
                            put("details", tx.details)
                            put("quantity", tx.quantity)
                            put("unitPrice", tx.unitPrice)
                            put("addition", tx.addition)
                            put("total", tx.total)
                            put("isPayment", tx.isPayment)
                            put("timestamp", tx.timestamp)
                            put("currency", tx.currency)
                            put("exchangeRate", tx.exchangeRate)
                        }
                        transactionsArray.put(txJson)
                    }
                    put("transactions", transactionsArray)
                }

                val backupDir = getApplication<Application>().filesDir
                val file = File(backupDir, "ProLedger_Backup_${System.currentTimeMillis() / 1000}.json")
                val fos = FileOutputStream(file)
                fos.write(rootJson.toString(2).toByteArray())
                fos.close()

                onCompleted(file)
            } catch (e: Exception) {
                e.printStackTrace()
                onCompleted(null)
            }
        }
    }

    // Database JSON backup restore
    fun restoreDatabaseJson(jsonString: String, onCompleted: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val rootJson = JSONObject(jsonString)
                val profileName = rootJson.optString("business_name", businessName.value)
                val profilePhone = rootJson.optString("business_phone", businessPhone.value)
                val profileAddress = rootJson.optString("business_address", businessAddress.value)

                updateBusinessProfile(profileName, profilePhone, profileAddress)

                val accountsJsonArray = rootJson.getJSONArray("accounts")
                val transactionsJsonArray = rootJson.getJSONArray("transactions")

                // Map old Account ID to new Account ID to prevent conflicts and ensure consistent FK
                val accountIdMapping = mutableMapOf<Int, Int>()

                // Restore Accounts
                for (i in 0 until accountsJsonArray.length()) {
                    val accObj = accountsJsonArray.getJSONObject(i)
                    val oldId = accObj.getInt("id")
                    val name = accObj.getString("name")
                    val phone = accObj.getString("phone")
                    val type = accObj.getString("type")
                    val createdAt = accObj.optLong("createdAt", System.currentTimeMillis())
                    val creditLimit = accObj.optDouble("creditLimit", 0.0)
                    val tag = accObj.optString("tag", "")
                    val initialBalance = accObj.optDouble("initialBalance", 0.0)

                    val account = Account(
                        name = name,
                        phone = phone,
                        type = type,
                        createdAt = createdAt,
                        creditLimit = creditLimit,
                        tag = tag,
                        initialBalance = initialBalance
                    )
                    val newId = repository.insertAccount(account).toInt()
                    accountIdMapping[oldId] = newId
                }

                // Restore Transactions
                for (i in 0 until transactionsJsonArray.length()) {
                    val txObj = transactionsJsonArray.getJSONObject(i)
                    val oldAccountId = txObj.getInt("accountId")
                    val newAccountId = accountIdMapping[oldAccountId]

                    if (newAccountId != null) {
                        val day = txObj.getString("day")
                        val date = txObj.getString("date")
                        val details = txObj.getString("details")
                        val quantity = txObj.getDouble("quantity")
                        val unitPrice = txObj.getDouble("unitPrice")
                        val addition = txObj.getDouble("addition")
                        val total = txObj.getDouble("total")
                        val isPayment = txObj.getBoolean("isPayment")
                        val timestamp = txObj.optLong("timestamp", System.currentTimeMillis())

                        val transaction = Transaction(
                            accountId = newAccountId,
                            day = day,
                            date = date,
                            details = details,
                            quantity = quantity,
                            unitPrice = unitPrice,
                            addition = addition,
                            total = total,
                            isPayment = isPayment,
                            timestamp = timestamp,
                            currency = txObj.optString("currency", "YER"),
                            exchangeRate = txObj.optDouble("exchangeRate", 1.0)
                        )
                        repository.insertTransaction(transaction)
                    }
                }

                onCompleted(true)
            } catch (e: Exception) {
                e.printStackTrace()
                onCompleted(false)
            }
        }
    }

    // Windows Local Sync Server and Manager
    private var syncServer: com.example.utils.WindowsSyncServer? = null

    private val _isServerRunning = MutableStateFlow(false)
    val isServerRunning: StateFlow<Boolean> = _isServerRunning.asStateFlow()

    private val _serverUrl = MutableStateFlow("")
    val serverUrl: StateFlow<String> = _serverUrl.asStateFlow()

    fun toggleSyncServer(enabled: Boolean) {
        if (enabled) {
            if (syncServer == null) {
                syncServer = com.example.utils.WindowsSyncServer(getApplication(), this)
            }
            syncServer?.start { running, url ->
                _isServerRunning.value = running
                _serverUrl.value = url
            }
        } else {
            syncServer?.stop { running ->
                _isServerRunning.value = running
                _serverUrl.value = ""
            }
        }
    }

    private val geminiRepository = com.example.data.repository.GeminiRepository()

    suspend fun parseSmartEntry(prompt: String, isOfflineMode: Boolean): Result<String> {
        return geminiRepository.parseSmartEntryTransaction(
            prompt = prompt,
            accounts = allAccounts.value,
            isOfflineMode = isOfflineMode
        )
    }

    fun loadInventory() {
        val json = prefs.getString("inventory_items", "[]") ?: "[]"
        try {
            val list = mutableListOf<InventoryItem>()
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    InventoryItem(
                        id = obj.getString("id"),
                        barcode = obj.getString("barcode"),
                        name = obj.getString("name"),
                        purchasePrice = obj.optDouble("purchasePrice", 0.0),
                        salePrice = obj.optDouble("salePrice", 0.0),
                        stockQuantity = obj.optDouble("stockQuantity", 0.0),
                        unit = obj.optString("unit", "حبة")
                    )
                )
            }
            _inventoryItems.value = list
        } catch (e: Exception) {
            e.printStackTrace()
            _inventoryItems.value = listOf()
        }
    }

    fun saveInventory(list: List<InventoryItem>) {
        val array = JSONArray()
        list.forEach { item ->
            val obj = JSONObject().apply {
                put("id", item.id)
                put("barcode", item.barcode)
                put("name", item.name)
                put("purchasePrice", item.purchasePrice)
                put("salePrice", item.salePrice)
                put("stockQuantity", item.stockQuantity)
                put("unit", item.unit)
            }
            array.put(obj)
        }
        prefs.edit().putString("inventory_items", array.toString()).apply()
        _inventoryItems.value = list
    }

    fun addInventoryItem(name: String, barcode: String, purchasePrice: Double, salePrice: Double, quantity: Double, unit: String) {
        val current = _inventoryItems.value.toMutableList()
        val existingIndex = current.indexOfFirst { it.barcode == barcode && barcode.isNotBlank() }
        if (existingIndex != -1) {
            val item = current[existingIndex]
            current[existingIndex] = item.copy(
                name = name,
                purchasePrice = purchasePrice,
                salePrice = salePrice,
                stockQuantity = item.stockQuantity + quantity,
                unit = unit
            )
        } else {
            current.add(InventoryItem(barcode = barcode, name = name, purchasePrice = purchasePrice, salePrice = salePrice, stockQuantity = quantity, unit = unit))
        }
        saveInventory(current)
    }

    fun updateInventoryItem(updated: InventoryItem) {
        val current = _inventoryItems.value.map { if (it.id == updated.id) updated else it }
        saveInventory(current)
    }

    fun deleteInventoryItem(id: String) {
        val current = _inventoryItems.value.filter { it.id != id }
        saveInventory(current)
    }

    fun sellItemFromStock(barcode: String, qty: Double) {
        val current = _inventoryItems.value.toMutableList()
        val index = current.indexOfFirst { it.barcode == barcode }
        if (index != -1) {
            val item = current[index]
            val newQty = (item.stockQuantity - qty).coerceAtLeast(0.0)
            current[index] = item.copy(stockQuantity = newQty)
            saveInventory(current)
        }
    }

    // --- Biometric Authentication Settings ---
    private val _isBiometricEnabled = MutableStateFlow(prefs.getBoolean("biometric_enabled", false))
    val isBiometricEnabled: StateFlow<Boolean> = _isBiometricEnabled.asStateFlow()

    fun updateBiometricSettings(enabled: Boolean) {
        prefs.edit().putBoolean("biometric_enabled", enabled).apply()
        _isBiometricEnabled.value = enabled
    }

    // --- Yemeni Quick Message Broadcasts ---
    fun getYemeniMessageTemplates(clientName: String, balance: Double, currency: String): List<String> {
        val formattedBal = String.format(Locale.US, "%,.2f", Math.abs(balance))
        val direction = if (balance >= 0) "مطلوب منكم سداد" else "لكم طرفنا رصيد دائن"
        val currLabel = when (currency) {
            "USD" -> "دولار"
            "SAR" -> "سعودي"
            "YER" -> "ريال يمني"
            else -> currency
        }
        val bizName = _businessName.value
        val bizPhone = _businessPhone.value
        
        return listOf(
            "عشاق ومستهلكي $bizName نود إخطاركم عميلنا المحترم [$clientName] بأن حسابكم الحالي هو ($formattedBal) $currLabel ($direction). شكراً لتعاملكم الراقي وثقتكم الدائمة بنا.",
            "إشعار مالي هام من [$bizName]: الأخ [$clientName]، يرجى الفحص والمراجعة؛ رصيد حسابكم لدينا حالياً هو ($formattedBal) $currLabel. للمراجعة أو الاستفسار اتصل بنا ($bizPhone).",
            "مرحباً [$clientName]، بموجب المطابقة الحسابية السنوية لشركة [$bizName]، يفيدكم نظامنا المحاسبي الآلي بأن رصيدكم المرحل هو ($formattedBal) $currLabel. دمتم ذخرًا لنا."
        )
    }

    // --- Dynamic Annual Fiscal Closing & Carryover ---
    fun performYearlyClosing(selectedClosingDate: String, onCompleted: (String) -> Unit) {
        viewModelScope.launch {
            val currentAccountsWithBalances = accountsWithBalance.value
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val dayFormat = SimpleDateFormat("EEEE", Locale("ar"))
            val todayStr = sdf.format(Date())
            val todayDayName = dayFormat.format(Date())

            for (accBal in currentAccountsWithBalances) {
                val accId = accBal.account.id
                val finalNetBal = accBal.balance

                repository.deleteTransactionsForAccount(accId)

                if (finalNetBal != 0.0) {
                    val openingTx = Transaction(
                        accountId = accId,
                        day = todayDayName,
                        date = selectedClosingDate.ifBlank { todayStr },
                        details = "📦 [رصيد مرحل وإقفال سنوي تلقائي للعام الجديد] لغاية $selectedClosingDate",
                        quantity = 1.0,
                        unitPrice = Math.abs(finalNetBal),
                        addition = 0.0,
                        total = Math.abs(finalNetBal),
                        isPayment = finalNetBal < 0,
                        currency = _defaultCurrency.value,
                        exchangeRate = 1.0,
                        dueDate = ""
                    )
                    repository.insertTransaction(openingTx)
                }
            }
            triggerAutoSync()
            onCompleted("تمت عملية الإقفال الحسابي السنوي بنجاح! تم تصفية كافة القيود والترحيل بأرصدة افتتاحية جديدة لـ ${currentAccountsWithBalances.size} حساب.")
        }
    }

    override fun onCleared() {
        super.onCleared()
        syncServer?.stop { }
    }
}

data class AccountWithBalance(
    val account: Account,
    val balance: Double,
    val transactionCount: Int
)

data class PdfTemplateConfig(
    val logo: String = "🏢",
    val showLogo: Boolean = true,
    val customTitle: String = "",
    val customSubtitle: String = "",
    val customFooter: String = "تم التوليد تلقائياً بواسطة تطبيق المحاسب anas برو (Pro Ledger)",
    val showSignature: Boolean = true,
    val fontStyle: String = "DEFAULT", // DEFAULT, MONOSPACE, SANS_SERIF, SERIF
    val fontSize: String = "MEDIUM", // SMALL, MEDIUM, LARGE
    val themeColor: String = "SLATE", // SLATE, NAVY, EMERALD, BURGUNDY, GOLDEN
    val colDetailsLabel: String = "التفاصيل والبيان",
    val colQtyVisible: Boolean = true,
    val colQtyLabel: String = "الكمية",
    val colPriceVisible: Boolean = true,
    val colPriceLabel: String = "السعر",
    val colAdditionVisible: Boolean = true,
    val colAdditionLabel: String = "الإضافي",
    val colTotalVisible: Boolean = true,
    val colTotalLabel: String = "الإجمالي"
)

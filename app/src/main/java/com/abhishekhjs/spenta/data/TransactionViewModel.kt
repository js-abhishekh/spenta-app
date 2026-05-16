package com.abhishekhjs.spenta.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TransactionViewModel(
    private val repository: TransactionRepository,
    val preferenceManager: PreferenceManager
) : ViewModel() {

    private val _initialBalance = MutableStateFlow(preferenceManager.getInitialBalance())
    val initialBalance: StateFlow<Double> = _initialBalance.asStateFlow()

    private val _currency = MutableStateFlow(preferenceManager.getCurrency())
    val currency: StateFlow<String> = _currency.asStateFlow()

    private val _userName = MutableStateFlow(preferenceManager.getUserName())
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _profileImage = MutableStateFlow(preferenceManager.getProfileImage())
    val profileImage: StateFlow<String> = _profileImage.asStateFlow()

    private val _themeMode = MutableStateFlow(preferenceManager.getThemeMode())
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _budgetType = MutableStateFlow(preferenceManager.getBudgetType())
    val budgetType: StateFlow<String> = _budgetType.asStateFlow()

    private val _budgetAmount = MutableStateFlow(preferenceManager.getBudgetAmount())
    val budgetAmount: StateFlow<Double> = _budgetAmount.asStateFlow()

    val allTransactions: StateFlow<List<Transaction>> = repository.allTransactions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allCategories: StateFlow<List<Category>> = repository.allCategories
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        // Pre-populate default categories if empty
        viewModelScope.launch {
            if (repository.getCategoryCount() == 0) {
                val defaults = listOf(
                    Category("Food", "Fastfood", isSystem = true),
                    Category("Shopping", "ShoppingCart", isSystem = true),
                    Category("Work", "Work", isSystem = true),
                    Category("Travel", "DirectionsCar", isSystem = true),
                    Category("Other", "Category", isSystem = true)
                )
                defaults.forEach { repository.insertCategory(it) }
            }
        }
    }

    fun insert(transaction: Transaction) = viewModelScope.launch {
        repository.insert(transaction)
    }

    fun update(transaction: Transaction) = viewModelScope.launch {
        repository.update(transaction)
    }

    fun delete(transaction: Transaction) = viewModelScope.launch {
        repository.delete(transaction)
    }

    fun insertCategory(category: Category) = viewModelScope.launch {
        repository.insertCategory(category)
    }

    fun deleteCategory(category: Category) = viewModelScope.launch {
        repository.deleteCategory(category)
    }

    fun clearAllData() = viewModelScope.launch {
        repository.clearAllTransactions()
    }

    fun setInitialBalance(balance: Double) {
        preferenceManager.setInitialBalance(balance)
        _initialBalance.value = balance
    }

    fun setCurrency(currency: String) {
        preferenceManager.setCurrency(currency)
        _currency.value = currency
    }

    fun setUserName(name: String) {
        preferenceManager.setUserName(name)
        _userName.value = name
    }

    fun setProfileImage(uri: String) {
        preferenceManager.setProfileImage(uri)
        _profileImage.value = uri
    }

    fun setThemeMode(mode: String) {
        preferenceManager.setThemeMode(mode)
        _themeMode.value = mode
    }

    fun setBudgetType(type: String) {
        preferenceManager.setBudgetType(type)
        _budgetType.value = type
    }

    fun setBudgetAmount(amount: Double) {
        preferenceManager.setBudgetAmount(amount)
        _budgetAmount.value = amount
    }

    fun exportToCsv(transactions: List<Transaction>): String {
        val builder = StringBuilder()
        builder.append("ID,Merchant,Amount,Category,Type,Timestamp\n")
        transactions.forEach { t ->
            builder.append("${t.id},\"${t.merchant}\",${t.amount},\"${t.category}\",\"${t.type}\",${t.timestamp}\n")
        }
        return builder.toString()
    }

    fun exportToJson(transactions: List<Transaction>): String {
        try {
            val jsonArray = org.json.JSONArray()
            transactions.forEach { t ->
                val obj = org.json.JSONObject()
                obj.put("merchant", t.merchant)
                obj.put("amount", t.amount)
                obj.put("category", t.category)
                obj.put("type", t.type)
                obj.put("timestamp", t.timestamp)
                obj.put("isPaid", t.isPaid)
                obj.put("isAcknowledged", t.isAcknowledged)
                jsonArray.put(obj)
            }
            return jsonArray.toString(2)
        } catch (e: Exception) {
            e.printStackTrace()
            return "[]"
        }
    }

    fun importFromJson(json: String) = viewModelScope.launch {
        try {
            val jsonArray = org.json.JSONArray(json)
            val transactions = mutableListOf<Transaction>()
            
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                transactions.add(
                    Transaction(
                        merchant = obj.getString("merchant"),
                        amount = obj.getDouble("amount"),
                        category = obj.getString("category"),
                        type = obj.getString("type"),
                        timestamp = obj.getLong("timestamp"),
                        isPaid = if (obj.has("isPaid")) obj.getBoolean("isPaid") else true,
                        isAcknowledged = if (obj.has("isAcknowledged")) obj.getBoolean("isAcknowledged") else true
                    )
                )
            }
            
            if (transactions.isNotEmpty()) {
                transactions.forEach { repository.insert(it) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

class TransactionViewModelFactory(
    private val repository: TransactionRepository,
    private val preferenceManager: PreferenceManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TransactionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TransactionViewModel(repository, preferenceManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

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
    private val preferenceManager: PreferenceManager
) : ViewModel() {

    private val _initialBalance = MutableStateFlow(preferenceManager.getInitialBalance())
    val initialBalance: StateFlow<Double> = _initialBalance.asStateFlow()

    private val _currency = MutableStateFlow(preferenceManager.getCurrency())
    val currency: StateFlow<String> = _currency.asStateFlow()

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

    fun exportToCsv(transactions: List<Transaction>): String {
        val builder = StringBuilder()
        builder.append("ID,Merchant,Amount,Category,Type,Timestamp\n")
        transactions.forEach { t ->
            builder.append("${t.id},\"${t.merchant}\",${t.amount},\"${t.category}\",\"${t.type}\",${t.timestamp}\n")
        }
        return builder.toString()
    }

    fun exportToJson(transactions: List<Transaction>): String {
        // Simple manual JSON construction to avoid adding GSON/Kotlinx Serialization if not present
        val builder = StringBuilder()
        builder.append("[\n")
        transactions.forEachIndexed { index, t ->
            builder.append("  {\n")
            builder.append("    \"id\": ${t.id},\n")
            builder.append("    \"merchant\": \"${t.merchant.replace("\"", "\\\"")}\",\n")
            builder.append("    \"amount\": ${t.amount},\n")
            builder.append("    \"category\": \"${t.category}\",\n")
            builder.append("    \"type\": \"${t.type}\",\n")
            builder.append("    \"timestamp\": ${t.timestamp}\n")
            builder.append("  }${if (index < transactions.size - 1) "," else ""}\n")
        }
        builder.append("]")
        return builder.toString()
    }

    fun importFromJson(json: String) = viewModelScope.launch {
        // Basic manual parsing (assuming the format we exported)
        // In a real app, use a proper JSON library
        try {
            val transactions = mutableListOf<Transaction>()
            val regex = Regex("""\{\s*"id":\s*(\d+),\s*"merchant":\s*"(.*?)",\s*"amount":\s*([\d.]+),\s*"category":\s*"(.*?)",\s*"type":\s*"(.*?)",\s*"timestamp":\s*(\d+)\s*\}""", RegexOption.DOT_MATCHES_ALL)
            val matches = regex.findAll(json)
            matches.forEach { match ->
                val groups = match.groupValues
                transactions.add(
                    Transaction(
                        merchant = groups[2].replace("\\\"", "\""),
                        amount = groups[3].toDouble(),
                        category = groups[4],
                        type = groups[5],
                        timestamp = groups[6].toLong()
                    )
                )
            }
            transactions.forEach { repository.insert(it) }
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

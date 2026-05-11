package com.abhishekhjs.spenta.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Double,
    val merchant: String,
    val category: String,
    val type: String, // "Income" or "Expense"
    val timestamp: Long = System.currentTimeMillis(),
    val isAcknowledged: Boolean = true,
    val isPaid: Boolean = true
)

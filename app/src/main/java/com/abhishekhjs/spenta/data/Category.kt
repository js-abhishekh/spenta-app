package com.abhishekhjs.spenta.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey
    val name: String,
    val iconName: String, // Store icon name as string for flexibility
    val isSystem: Boolean = false // Prevent deletion of default categories
)

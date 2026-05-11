package com.abhishekhjs.spenta

import android.app.Application
import com.abhishekhjs.spenta.data.AppDatabase
import com.abhishekhjs.spenta.data.PreferenceManager
import com.abhishekhjs.spenta.data.TransactionRepository
import com.abhishekhjs.spenta.nearby.NearbyManager

class SpentaApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
    val preferenceManager: PreferenceManager by lazy { PreferenceManager(this) }
    val repository: TransactionRepository by lazy { TransactionRepository(database.transactionDao(), database.categoryDao()) }
    val nearbyManager: NearbyManager by lazy { NearbyManager(this) }
}

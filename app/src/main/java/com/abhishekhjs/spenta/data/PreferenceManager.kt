package com.abhishekhjs.spenta.data

import android.content.Context
import android.content.SharedPreferences

class PreferenceManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("spenta_prefs", Context.MODE_PRIVATE)

    fun setInitialBalance(balance: Double) {
        prefs.edit().putFloat("initial_balance", balance.toFloat()).apply()
    }

    fun getInitialBalance(): Double {
        return prefs.getFloat("initial_balance", 0.0f).toDouble()
    }

    fun setCurrency(currency: String) {
        prefs.edit().putString("currency", currency).apply()
    }

    fun getCurrency(): String {
        return prefs.getString("currency", "₹") ?: "₹"
    }

    fun setFirstRun(isFirstRun: Boolean) {
        prefs.edit().putBoolean("is_first_run", isFirstRun).apply()
    }

    fun isFirstRun(): Boolean {
        return prefs.getBoolean("is_first_run", true)
    }
}

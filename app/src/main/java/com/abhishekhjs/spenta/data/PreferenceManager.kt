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

    fun setSetupComplete(complete: Boolean) {
        prefs.edit().putBoolean("setup_complete", complete).apply()
    }

    fun isSetupComplete(): Boolean {
        return prefs.getBoolean("setup_complete", false)
    }

    fun setFirstRun(isFirstRun: Boolean) {
        prefs.edit().putBoolean("is_first_run", isFirstRun).apply()
    }

    fun isFirstRun(): Boolean {
        return prefs.getBoolean("is_first_run", true)
    }

    fun setUserName(name: String) {
        prefs.edit().putString("user_name", name).apply()
    }

    fun getUserName(): String {
        return prefs.getString("user_name", "") ?: ""
    }

    fun setProfileImage(uri: String) {
        prefs.edit().putString("profile_image", uri).apply()
    }

    fun getProfileImage(): String {
        return prefs.getString("profile_image", "") ?: ""
    }

    fun setThemeMode(mode: String) {
        prefs.edit().putString("theme_mode", mode).apply()
    }

    fun getThemeMode(): String {
        return prefs.getString("theme_mode", "System") ?: "System"
    }

    fun setBudgetType(type: String) {
        prefs.edit().putString("budget_type", type).apply()
    }

    fun getBudgetType(): String {
        return prefs.getString("budget_type", "Monthly") ?: "Monthly"
    }

    fun setBudgetAmount(amount: Double) {
        prefs.edit().putFloat("budget_amount", amount.toFloat()).apply()
    }

    fun getBudgetAmount(): Double {
        return prefs.getFloat("budget_amount", 0.0f).toDouble()
    }
}

package com.abhishekhjs.spenta.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import android.util.Log
import com.abhishekhjs.spenta.SpentaApplication
import com.abhishekhjs.spenta.data.Transaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SpentaNotificationService : NotificationListenerService() {

    companion object {
        private const val TAG = "SpentaNotifService"
        private const val CHANNEL_ID = "spenta_notifications"
        const val KEY_TEXT_REPLY = "key_text_reply"
        const val EXTRA_AMOUNT = "extra_amount"
        const val EXTRA_MERCHANT = "extra_merchant"
        const val EXTRA_CATEGORY = "extra_category"
        const val EXTRA_TYPE = "extra_type"
        const val ACTION_SAVE_MERCHANT = "com.abhishekhjs.spenta.ACTION_SAVE_MERCHANT"
        const val ACTION_CATEGORIZE = "com.abhishekhjs.spenta.ACTION_CATEGORIZE"
        const val ACTION_TEST_NOTIFICATION = "com.abhishekhjs.spenta.ACTION_TEST_NOTIFICATION"
    }

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    private var lastProcessedAmount: Double = -1.0
    private var lastProcessedTime: Long = 0

    private val currencyRegex = Regex("(?:₹|rs\\.?|inr|\\$|€|£|¥)\\s*([\\d,]+(?:\\.\\d{1,2})?)", RegexOption.IGNORE_CASE)

    private fun getCurrencySymbol(): String {
        return (application as SpentaApplication).preferenceManager.getCurrency()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        
        if (sbn == null || sbn.packageName == packageName) return

        val extras = sbn.notification.extras
        val title = extras.getString("android.title") ?: ""
        val text = extras.getCharSequence("android.text")?.toString() ?: ""
        val bigText = extras.getCharSequence("android.bigText")?.toString() ?: ""
        
        val fullContent = "$title $text $bigText".lowercase()

        // 1. The "Kill Switch" Words (Ignore these notifications completely)
        val ignoreWords = listOf("balance", "due", "loan", "offer", "otp", "code", "minimum")
        if (ignoreWords.any { fullContent.contains(it) }) {
            return 
        }

        // 2. The "Action" Words (It must contain at least one of these)
        val actionWords = listOf("debited", "credited", "paid", "spent", "sent to", "received", "deducted", "transaction")
        if (actionWords.none { fullContent.contains(it) }) {
            return 
        }

        // 3. NOW run the refined Regex to safely grab the number
        val match = currencyRegex.find(fullContent)

        if (match != null) {
            val rawAmountString = match.groupValues[1].replace(",", "")
            val amount = rawAmountString.toDoubleOrNull() ?: 0.0

            // Deduplication: Ignore if same amount notification received within 2 seconds
            val currentTime = System.currentTimeMillis()
            if (amount == lastProcessedAmount && (currentTime - lastProcessedTime) < 2000) {
                Log.d(TAG, "Duplicate notification detected for amount: $amount, skipping.")
                return
            }
            lastProcessedAmount = amount
            lastProcessedTime = currentTime

            Log.d(TAG, "Detected amount: $amount from ${sbn.packageName}")
            
            // Detect type
            val incomeWords = listOf("credited", "received")
            val type = if (incomeWords.any { fullContent.contains(it) }) "Income" else "Expense"

            // Step 0: Auto-add to database as unacknowledged
            val repository = (application as SpentaApplication).repository
            serviceScope.launch {
                repository.insert(
                    Transaction(
                        amount = amount,
                        merchant = "",
                        category = "",
                        type = type,
                        isAcknowledged = false
                    )
                )
            }

            // Flow step 1: Ask for Merchant via text input
            showMerchantPromptNotification(rawAmountString, type)
        }
    }

    private fun showMerchantPromptNotification(amount: String, type: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Spenta Categorization",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val remoteInput = RemoteInput.Builder(KEY_TEXT_REPLY)
            .setLabel("Where did you ${if(type == "Income") "receive" else "spend"} ${getCurrencySymbol()}$amount?")
            .build()

        val resultIntent = Intent(this, SpentaNotificationService::class.java).apply {
            action = ACTION_SAVE_MERCHANT
            putExtra(EXTRA_AMOUNT, amount)
            putExtra(EXTRA_TYPE, type)
        }

        val resultPendingIntent = PendingIntent.getService(
            this,
            amount.hashCode(),
            resultIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        val action = NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_edit,
            "Enter Merchant",
            resultPendingIntent
        )
            .addRemoteInput(remoteInput)
            .build()

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_notification_overlay)
            .setContentTitle("New Spenta: ${getCurrencySymbol()}$amount")
            .setContentText("Tap to enter merchant name.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(false)
            .setOngoing(true)
            .addAction(action)
            .build()

        notificationManager.notify(amount.hashCode(), notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SAVE_MERCHANT -> handleMerchantInput(intent)
            ACTION_CATEGORIZE -> handleCategorySelection(intent)
            ACTION_TEST_NOTIFICATION -> handleTestNotification()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun handleTestNotification() {
        val amount = 1250.0
        val rawAmountString = "1250"
        val type = "Expense"
        
        val repository = (application as SpentaApplication).repository
        serviceScope.launch {
            repository.insert(
                Transaction(
                    amount = amount,
                    merchant = "",
                    category = "",
                    type = type,
                    isAcknowledged = false
                )
            )
        }
        showMerchantPromptNotification(rawAmountString, type)
    }

    private fun handleMerchantInput(intent: Intent) {
        val remoteInput = RemoteInput.getResultsFromIntent(intent)
        val amount = intent.getStringExtra(EXTRA_AMOUNT)
        val type = intent.getStringExtra(EXTRA_TYPE) ?: "Expense"
        if (remoteInput != null && amount != null) {
            val merchant = remoteInput.getCharSequence(KEY_TEXT_REPLY).toString()
            // Flow step 2: Show category buttons
            showCategoryButtonsNotification(amount, merchant, type)
        }
    }

    private fun showCategoryButtonsNotification(amount: String, merchant: String, type: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_notification_overlay)
            .setContentTitle("Categorize ${getCurrencySymbol()}$amount at $merchant")
            .setContentText("Select a category:")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(false)
            .setOngoing(true)

        // Add a text input for custom category
        val remoteInput = RemoteInput.Builder(KEY_TEXT_REPLY)
            .setLabel("Enter custom category")
            .build()
            
        val customIntent = Intent(this, SpentaNotificationService::class.java).apply {
            action = ACTION_CATEGORIZE
            putExtra(EXTRA_AMOUNT, amount)
            putExtra(EXTRA_MERCHANT, merchant)
            putExtra(EXTRA_TYPE, type)
        }
        
        val customPendingIntent = PendingIntent.getService(
            this,
            (amount + merchant + "custom").hashCode(),
            customIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        builder.addAction(
            NotificationCompat.Action.Builder(
                android.R.drawable.ic_menu_edit,
                "Custom",
                customPendingIntent
            ).addRemoteInput(remoteInput).build()
        )

        // Category buttons
        val categories = listOf("Food", "Shopping", "Bills", "Health", "Entertainment")
        categories.forEach { category ->
            val intent = Intent(this, SpentaNotificationService::class.java).apply {
                action = ACTION_CATEGORIZE
                putExtra(EXTRA_AMOUNT, amount)
                putExtra(EXTRA_MERCHANT, merchant)
                putExtra(EXTRA_CATEGORY, category)
                putExtra(EXTRA_TYPE, type)
            }
            val pendingIntent = PendingIntent.getService(
                this,
                (amount + merchant + category).hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            builder.addAction(0, category, pendingIntent)
        }

        notificationManager.notify(amount.hashCode(), builder.build())
    }

    private fun handleCategorySelection(intent: Intent) {
        val amountStr = intent.getStringExtra(EXTRA_AMOUNT)
        val merchant = intent.getStringExtra(EXTRA_MERCHANT)
        val type = intent.getStringExtra(EXTRA_TYPE) ?: "Expense"
        
        // Try to get category from extra (button click) or from remote input (custom entry)
        var category = intent.getStringExtra(EXTRA_CATEGORY)
        val remoteInput = RemoteInput.getResultsFromIntent(intent)
        if (remoteInput != null) {
            category = remoteInput.getCharSequence(KEY_TEXT_REPLY).toString()
        }

        if (amountStr != null && merchant != null && category != null) {
            val amount = amountStr.toDoubleOrNull() ?: 0.0
            Log.d(TAG, "Updating/Saving: $amount for $merchant as $category")
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(amountStr.hashCode())
            
            val repository = (application as SpentaApplication).repository
            serviceScope.launch {
                // Look for the unacknowledged transaction created when the notification was first posted
                // Look back 5 minutes to be safe
                val since = System.currentTimeMillis() - (5 * 60 * 1000)
                val existing = repository.findUnacknowledged(amount, since)
                
                if (existing != null) {
                    repository.update(
                        existing.copy(
                            merchant = merchant,
                            category = category,
                            type = type,
                            isAcknowledged = true
                        )
                    )
                } else {
                    repository.insert(
                        Transaction(
                            amount = amount,
                            merchant = merchant,
                            category = category,
                            type = type,
                            isAcknowledged = true
                        )
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }
}

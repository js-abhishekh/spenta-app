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
        const val ACTION_SPLIT_BILL = "com.abhishekhjs.spenta.ACTION_SPLIT_BILL"
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
        val title = extras.getCharSequence("android.title")?.toString() ?: ""
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

            // Try to extract merchant
            val extractedMerchant = extractMerchant(fullContent)

            // Step 0: Auto-add to database as unacknowledged
            val repository = (application as SpentaApplication).repository
            serviceScope.launch {
                repository.insert(
                    Transaction(
                        amount = amount,
                        merchant = extractedMerchant,
                        category = "",
                        type = type,
                        isAcknowledged = false
                    )
                )
            }

            // Flow step 1: Ask for Merchant via text input (pre-fill label if merchant found)
            showMerchantPromptNotification(rawAmountString, type, extractedMerchant)
        }
    }

    private fun extractMerchant(text: String): String {
        // Clean text: remove numbers that look like account endings or dates
        val cleanText = text.replace(Regex("\\b[0-9xX]{4,16}\\b"), " ")
        
        val patterns = listOf(
            Regex("(?:to|at|towards|paid to)\\s+([a-z0-9\\s]{3,25})(?:\\s|\\.|$)", RegexOption.IGNORE_CASE),
            Regex("sent\\s+to\\s+([a-z0-9\\s]{3,25})", RegexOption.IGNORE_CASE),
            Regex("spent\\s+(?:on|at)\\s+([a-z0-9\\s]{3,25})", RegexOption.IGNORE_CASE),
            Regex("vpa\\s+([a-z0-9.\\-_]{3,30}@[a-z]{3,10})", RegexOption.IGNORE_CASE) // UPI VPA
        )
        
        for (regex in patterns) {
            val match = regex.find(cleanText)
            if (match != null) {
                val candidate = match.groupValues[1].trim()
                val ignore = listOf("rs", "inr", "your", "my", "account", "a/c", "bank", "balance", "debited", "credited")
                if (ignore.none { candidate.lowercase().contains(it) } && candidate.length > 2) {
                    // Take first 2-3 words to avoid long sentences being captured
                    return candidate.split(Regex("\\s+")).take(3).joinToString(" ")
                }
            }
        }
        return ""
    }

    private fun showMerchantPromptNotification(amount: String, type: String, merchant: String = "") {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Spenta Categorization",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val label = if (merchant.isNotEmpty()) {
            "Confirm: $merchant?"
        } else {
            "Where did you ${if(type == "Income") "receive" else "spend"} ${getCurrencySymbol()}$amount?"
        }

        val remoteInput = RemoteInput.Builder(KEY_TEXT_REPLY)
            .setLabel(label)
            .build()

        val resultIntent = Intent(this, SpentaNotificationService::class.java).apply {
            setAction(ACTION_SAVE_MERCHANT)
            putExtra(EXTRA_AMOUNT, amount)
            putExtra(EXTRA_TYPE, type)
        }

        val resultPendingIntent = PendingIntent.getService(
            this,
            amount.hashCode(),
            resultIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        val merchantAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_edit,
            if (merchant.isNotEmpty()) "Edit Merchant" else "Enter Merchant",
            resultPendingIntent
        )
            .addRemoteInput(remoteInput)
            .build()

        val contentTitle = if (merchant.isNotEmpty()) {
            "${getCurrencySymbol()}$amount at $merchant"
        } else {
            "New Spenta: ${getCurrencySymbol()}$amount"
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_notification_overlay)
            .setContentTitle(contentTitle)
            .setContentText("Tap to enter merchant name and categorize.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(false)
            .setOngoing(true)
            .addAction(merchantAction)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText(if (merchant.isNotEmpty()) "We detected a transaction of ${getCurrencySymbol()}$amount at $merchant. Tap to confirm or edit merchant name." else "Enter merchant name to categorize this transaction."))
            .build()

        notificationManager.notify(amount.hashCode(), notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SAVE_MERCHANT -> handleMerchantInput(intent)
            ACTION_CATEGORIZE -> handleCategorySelection(intent)
            ACTION_TEST_NOTIFICATION -> handleTestNotification()
            ACTION_SPLIT_BILL -> handleSplitBill(intent)
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun handleSplitBill(intent: Intent) {
        val amount = intent.getStringExtra(EXTRA_AMOUNT)
        
        // Bring user to the app's split screen
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            putExtra("navigate_to", "split_bill")
            putExtra("amount", amount)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(launchIntent)
        
        // Dismiss notification using the amount hash that was used to notify
        val originalAmount = intent.getStringExtra(EXTRA_AMOUNT)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(originalAmount?.hashCode() ?: 0)
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

        // 1. Add Custom Category Action
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

        // 3. Category buttons
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

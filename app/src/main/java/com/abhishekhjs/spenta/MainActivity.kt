package com.abhishekhjs.spenta

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.abhishekhjs.spenta.ui.theme.Inter
import com.abhishekhjs.spenta.data.Transaction
import com.abhishekhjs.spenta.data.TransactionViewModel
import com.abhishekhjs.spenta.data.TransactionViewModelFactory
import com.abhishekhjs.spenta.ui.Screen
import com.abhishekhjs.spenta.ui.items
import com.abhishekhjs.spenta.ui.screens.CategorySettingsScreen
import com.abhishekhjs.spenta.ui.screens.HomeScreen
import com.abhishekhjs.spenta.ui.screens.OnboardingScreen
import com.abhishekhjs.spenta.ui.screens.SettingsScreen
import com.abhishekhjs.spenta.ui.screens.SpendingsScreen
import com.abhishekhjs.spenta.ui.screens.SplitBillScreen
import com.abhishekhjs.spenta.ui.screens.SplashScreen
import com.abhishekhjs.spenta.ui.theme.SpentaTheme

class MainActivity : ComponentActivity() {
    private val viewModel: TransactionViewModel by viewModels {
        val app = application as SpentaApplication
        TransactionViewModelFactory(app.repository, app.preferenceManager)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val darkTheme = when (themeMode) {
                "Light" -> false
                "Dark" -> true
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }
            SpentaTheme(darkTheme = darkTheme) {
                PermissionHandler()
                MainScreen(viewModel, (application as SpentaApplication).preferenceManager)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}

@Composable
fun PermissionHandler() {
    val context = LocalContext.current
    
    val permissionsToRequest = mutableListOf(
        Manifest.permission.CAMERA,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
            add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            add(Manifest.permission.BLUETOOTH_ADVERTISE)
            add(Manifest.permission.BLUETOOTH_SCAN)
            add(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            // Below Android 12, Bluetooth permissions are handled differently but often covered by location
            // However, it's good practice to include them if the app explicitly uses Bluetooth APIs
            add(Manifest.permission.BLUETOOTH)
            add(Manifest.permission.BLUETOOTH_ADMIN)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            checkAndShowNotificationListenerPrompt(context)
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(permissionsToRequest.toTypedArray())
    }
}

fun isNotificationServiceEnabled(context: Context): Boolean {
    val pkgName = context.packageName
    val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
    if (!TextUtils.isEmpty(flat)) {
        val names = flat.split(":").toTypedArray()
        for (name in names) {
            val cn = ComponentName.unflattenFromString(name)
            if (cn != null) {
                if (TextUtils.equals(pkgName, cn.packageName)) {
                    return true
                }
            }
        }
    }
    return false
}

fun checkAndShowNotificationListenerPrompt(context: Context) {
    if (!isNotificationServiceEnabled(context)) {
        showSystemNotification(context)
    }
}

private fun showSystemNotification(context: Context) {
    val channelId = "permission_channel"
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            channelId,
            "Permission Alerts",
            NotificationManager.IMPORTANCE_HIGH
        )
        notificationManager.createNotificationChannel(channel)
    }

    val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
    val pendingIntent = PendingIntent.getActivity(
        context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    val notification = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(android.R.drawable.ic_dialog_alert)
        .setContentTitle("Action Required: Enable Spenta")
        .setContentText("Tap to enable notification access for expense detection.")
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .setContentIntent(pendingIntent)
        .build()

    notificationManager.notify(1001, notification)
}

fun showSplitReceivedNotification(context: Context, amount: Double, merchant: String, senderName: String) {
    val channelId = "spenta_notifications"
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val currencySymbol = (context.applicationContext as SpentaApplication).preferenceManager.getCurrency()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            channelId,
            "Spenta Notifications",
            NotificationManager.IMPORTANCE_HIGH
        )
        notificationManager.createNotificationChannel(channel)
    }

    val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
    val pendingIntent = PendingIntent.getActivity(
        context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    val notification = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(android.R.drawable.ic_menu_share)
        .setContentTitle("New Split Request Received")
        .setContentText("$senderName requested $currencySymbol$amount for $merchant")
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .setContentIntent(pendingIntent)
        .setStyle(NotificationCompat.BigTextStyle()
            .bigText("$senderName has shared a split with you for \"$merchant\". An expense of $currencySymbol$amount has been added to your transactions."))
        .build()

    notificationManager.notify(System.currentTimeMillis().toInt(), notification)
}

@Composable
fun MainScreen(viewModel: TransactionViewModel, preferenceManager: com.abhishekhjs.spenta.data.PreferenceManager) {
    val context = LocalContext.current
    val navController = rememberNavController()
    var showAddDialog by remember { mutableStateOf(false) }

    // Handle incoming navigation intents and permission re-check
    val activity = context as? ComponentActivity
    var isPermissionGranted by remember { mutableStateOf(isNotificationServiceEnabled(context)) }

    DisposableEffect(activity) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                isPermissionGranted = isNotificationServiceEnabled(context)
                
                activity?.intent?.let { intent ->
                    val navigateTo = intent.getStringExtra("navigate_to")
                    if (navigateTo == "split_bill") {
                        val amount = intent.getStringExtra("amount") ?: ""
                        val merchant = intent.getStringExtra("merchant") ?: ""
                        val encodedAmount = java.net.URLEncoder.encode(amount, "UTF-8")
                        val encodedMerchant = java.net.URLEncoder.encode(merchant, "UTF-8")
                        navController.navigate("split_bill?amount=$encodedAmount&merchant=$encodedMerchant") {
                            launchSingleTop = true
                        }
                        intent.removeExtra("navigate_to")
                    }
                }
            }
        }
        activity?.lifecycle?.addObserver(observer)
        onDispose {
            activity?.lifecycle?.removeObserver(observer)
        }
    }

    // Handle incoming Nearby payloads
    val nearbyManager = (context.applicationContext as SpentaApplication).nearbyManager
    val incomingPayload by nearbyManager.incomingPayloads.collectAsState()

    LaunchedEffect(incomingPayload) {
        incomingPayload?.let { payload ->
            if (payload.startsWith("SPLIT|")) {
                val parts = payload.split("|")
                if (parts.size >= 3) {
                    val amount = parts[1].toDoubleOrNull() ?: 0.0
                    val merchant = parts[2]
                    val senderName = if (parts.size >= 4) parts[3] else "Friend"
                    viewModel.insert(
                        Transaction(
                            amount = amount,
                            merchant = "Split: $merchant ($senderName)",
                            category = "Split",
                            type = "Expense",
                            isPaid = false,
                            isAcknowledged = false
                        )
                    )
                    showSplitReceivedNotification(context, amount, merchant, senderName)
                }
                nearbyManager.clearIncomingPayload()
            }
        }
    }
    
    val isFirstRun = remember { preferenceManager.isFirstRun() }
    
    Scaffold(
        floatingActionButton = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            
            // Only show the "Add Transaction" FAB on Home and Spendings screens
            if (currentRoute == Screen.Home.route || currentRoute == Screen.Spendings.route) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Transaction")
                }
            }
        },
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            if (items.any { it.route == currentRoute }) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp
                ) {
                    val currentDestination = navBackStackEntry?.destination
                    items.forEach { screen ->
                        val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = null) },
                            label = { Text(screen.title, fontFamily = Inter) },
                            selected = selected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Permission Banner (UI integration) - Hide on Splash/Onboarding
            AnimatedVisibility(visible = !isPermissionGranted && currentRoute != Screen.Splash.route && currentRoute != Screen.Onboarding.route) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Notification Access Required",
                                fontWeight = FontWeight.Bold,
                                fontFamily = Inter,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = "Tap here to enable notification detection in settings.",
                                fontSize = 12.sp,
                                fontFamily = Inter,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            NavHost(
                navController = navController,
                startDestination = Screen.Splash.route,
                modifier = Modifier.weight(1f)
            ) {
                composable(Screen.Splash.route) {
                    SplashScreen(onTimeout = {
                        val destination = if (!preferenceManager.isSetupComplete()) Screen.Onboarding.route else Screen.Home.route
                        navController.navigate(destination) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    })
                }
                composable(Screen.Onboarding.route) {
                    OnboardingScreen(
                        viewModel = viewModel,
                        onFinish = {
                            preferenceManager.setSetupComplete(true)
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Onboarding.route) { inclusive = true }
                            }
                        }
                    )
                }
                composable(Screen.Home.route) { 
                    HomeScreen(
                        viewModel = viewModel,
                        onNavigateToSplit = { navController.navigate(Screen.SplitBill.route) },
                        onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                        onAddTransaction = { showAddDialog = true }
                    ) 
                }
                composable(Screen.Spendings.route) { 
                    SpendingsScreen(
                        viewModel = viewModel,
                        onAddTransaction = { showAddDialog = true }
                    ) 
                }
                composable(Screen.Settings.route) { 
                    SettingsScreen(
                        viewModel = viewModel,
                        onNavigateToCategories = { navController.navigate("category_settings") }
                    ) 
                }
                composable("category_settings") {
                    CategorySettingsScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(
                    route = Screen.SplitBill.route,
                    arguments = listOf(
                        navArgument("amount") { nullable = true },
                        navArgument("merchant") { nullable = true }
                    )
                ) { backStackEntry ->
                    val amount = backStackEntry.arguments?.getString("amount")
                    val merchant = backStackEntry.arguments?.getString("merchant")
                    SplitBillScreen(
                        viewModel = viewModel,
                        nearbyManager = (context.applicationContext as SpentaApplication).nearbyManager,
                        amount = amount,
                        merchant = merchant,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }

        if (showAddDialog) {
            AddTransactionDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { merchant, amount, category, type ->
                    viewModel.insert(
                        Transaction(
                            merchant = merchant,
                            amount = amount.toDoubleOrNull() ?: 0.0,
                            category = category,
                            type = type,
                            isPaid = true
                        )
                    )
                    showAddDialog = false
                },
                viewModel = viewModel
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddTransactionDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String) -> Unit,
    viewModel: TransactionViewModel
) {
    var merchant by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    val categoriesFromDb by viewModel.allCategories.collectAsState()
    var category by remember { mutableStateOf("Food") }
    var type by remember { mutableStateOf("Expense") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Transaction", fontFamily = Inter) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = merchant,
                    onValueChange = { merchant = it },
                    label = { Text("Merchant", fontFamily = Inter) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount", fontFamily = Inter) },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Text("Type", style = MaterialTheme.typography.labelLarge, fontFamily = Inter)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = type == "Expense",
                        onClick = { type = "Expense" },
                        label = { Text("Expense", fontFamily = Inter) }
                    )
                    FilterChip(
                        selected = type == "Income",
                        onClick = { type = "Income" },
                        label = { Text("Income", fontFamily = Inter) }
                    )
                }

                Text("Category", style = MaterialTheme.typography.labelLarge, fontFamily = Inter)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    categoriesFromDb.forEach { cat ->
                        FilterChip(
                            selected = category == cat.name,
                            onClick = { category = cat.name },
                            label = { Text(cat.name, fontFamily = Inter) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(merchant, amount, category, type) }) {
                Text("Add", fontFamily = Inter)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", fontFamily = Inter)
            }
        }
    )
}

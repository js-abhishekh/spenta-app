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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
            SpentaTheme {
                PermissionHandler()
                MainScreen(viewModel, (application as SpentaApplication).preferenceManager)
            }
        }
    }
}

@Composable
fun PermissionHandler() {
    val context = LocalContext.current
    
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            checkAndShowNotificationListenerPrompt(context)
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                checkAndShowNotificationListenerPrompt(context)
            }
        } else {
            checkAndShowNotificationListenerPrompt(context)
        }
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

@Composable
fun MainScreen(viewModel: TransactionViewModel, preferenceManager: com.abhishekhjs.spenta.data.PreferenceManager) {
    val context = LocalContext.current
    val navController = rememberNavController()
    var showAddDialog by remember { mutableStateOf(false) }
    
    val isFirstRun = remember { preferenceManager.isFirstRun() }
    
    var isPermissionGranted by remember { mutableStateOf(isNotificationServiceEnabled(context)) }

    // Re-check permission when resuming the app
    DisposableEffect(Unit) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                isPermissionGranted = isNotificationServiceEnabled(context)
            }
        }
        (context as ComponentActivity).lifecycle.addObserver(observer)
        onDispose {
            (context as ComponentActivity).lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            
            // Hide BottomBar on Splash and Onboarding screens
            if (currentRoute != Screen.Splash.route && currentRoute != Screen.Onboarding.route) {
                NavigationBar {
                    val currentDestination = navBackStackEntry?.destination
                    items.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = null) },
                            label = { Text(screen.title) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
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
        },
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
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route

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
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = "Tap here to enable notification detection in settings.",
                                fontSize = 12.sp,
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
                        val destination = if (preferenceManager.isFirstRun()) Screen.Onboarding.route else Screen.Home.route
                        navController.navigate(destination) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    })
                }
                composable(Screen.Onboarding.route) {
                    OnboardingScreen(
                        viewModel = viewModel,
                        onFinish = {
                            preferenceManager.setFirstRun(false)
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Onboarding.route) { inclusive = true }
                            }
                        }
                    )
                }
                composable(Screen.Home.route) { HomeScreen(viewModel) }
                composable(Screen.Spendings.route) { SpendingsScreen(viewModel) }
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
                            type = type
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
        title = { Text("Add Transaction") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = merchant,
                    onValueChange = { merchant = it },
                    label = { Text("Merchant") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Text("Type", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = type == "Expense",
                        onClick = { type = "Expense" },
                        label = { Text("Expense") }
                    )
                    FilterChip(
                        selected = type == "Income",
                        onClick = { type = "Income" },
                        label = { Text("Income") }
                    )
                }

                Text("Category", style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    categoriesFromDb.forEach { cat ->
                        FilterChip(
                            selected = category == cat.name,
                            onClick = { category = cat.name },
                            label = { Text(cat.name) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(merchant, amount, category, type) }) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

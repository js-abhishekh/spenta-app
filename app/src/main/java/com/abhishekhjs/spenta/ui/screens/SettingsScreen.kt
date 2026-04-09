package com.abhishekhjs.spenta.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abhishekhjs.spenta.data.TransactionViewModel
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun SettingsScreen(
    viewModel: TransactionViewModel,
    onNavigateToCategories: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val transactions by viewModel.allTransactions.collectAsState()
    val currentBalance by viewModel.initialBalance.collectAsState()
    val currentCurrency by viewModel.currency.collectAsState()
    
    var showClearDialog by remember { mutableStateOf(false) }
    var showBalanceDialog by remember { mutableStateOf(false) }
    var showCurrencyDialog by remember { mutableStateOf(false) }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let {
            coroutineScope.launch {
                context.contentResolver.openOutputStream(it)?.use { stream ->
                    stream.write(viewModel.exportToCsv(transactions).toByteArray())
                }
            }
        }
    }

    val createBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            coroutineScope.launch {
                context.contentResolver.openOutputStream(it)?.use { stream ->
                    stream.write(viewModel.exportToJson(transactions).toByteArray())
                }
            }
        }
    }

    val importBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            coroutineScope.launch {
                context.contentResolver.openInputStream(it)?.bufferedReader()?.use { reader ->
                    val json = reader.readText()
                    viewModel.importFromJson(json)
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Profile & Balance Section
        SettingsGroup(title = "Financial Profile") {
            SettingsActionItem(
                title = "Current Bank Balance",
                subtitle = "Set your starting balance: $currentCurrency${String.format(Locale.US, "%.2f", currentBalance)}",
                icon = Icons.Default.AccountBalanceWallet,
                onClick = { showBalanceDialog = true }
            )
            SettingsActionItem(
                title = "Currency Unit",
                subtitle = "Current: $currentCurrency",
                icon = Icons.Default.CurrencyExchange,
                onClick = { showCurrencyDialog = true }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Customization Section
        SettingsGroup(title = "Personalization") {
            SettingsActionItem(
                title = "Custom Categories",
                subtitle = "Rent, Gym, Tech, and more",
                icon = Icons.Default.Category,
                indicatorColor = Color(0xFF4CAF50),
                onClick = onNavigateToCategories
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Data Management Section
        SettingsGroup(title = "Data Management") {
            SettingsActionItem(
                title = "Export Ledger",
                subtitle = "Save data for Excel or Sheets (.csv)",
                icon = Icons.Default.Download,
                iconTint = Color(0xFF4CAF50),
                onClick = { createDocumentLauncher.launch("spenta_ledger.csv") }
            )
            SettingsActionItem(
                title = "Backup to File",
                subtitle = "Export all data to a .json file",
                icon = Icons.Default.Backup,
                onClick = { createBackupLauncher.launch("spenta_backup.json") }
            )
            SettingsActionItem(
                title = "Restore from Backup",
                subtitle = "Import data from a .json file",
                icon = Icons.Default.Restore,
                onClick = { importBackupLauncher.launch(arrayOf("application/json")) }
            )
            SettingsActionItem(
                title = "Clear All Data",
                subtitle = "Permanently delete all transaction logs",
                icon = Icons.Default.DeleteForever,
                iconTint = Color(0xFFE57373),
                titleColor = Color(0xFFE57373),
                onClick = { showClearDialog = true }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "Spenta v1.0.0",
            modifier = Modifier.align(Alignment.CenterHorizontally),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear All Data") },
            text = { Text("Are you sure you want to delete all transaction records? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllData()
                        showClearDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showBalanceDialog) {
        var balanceText by remember { mutableStateOf(currentBalance.toString()) }
        AlertDialog(
            onDismissRequest = { showBalanceDialog = false },
            title = { Text("Set Starting Balance") },
            text = {
                OutlinedTextField(
                    value = balanceText,
                    onValueChange = { balanceText = it },
                    label = { Text("Balance") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    balanceText.toDoubleOrNull()?.let { viewModel.setInitialBalance(it) }
                    showBalanceDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBalanceDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showCurrencyDialog) {
        val currencies = listOf("₹", "$", "€", "£", "¥")
        AlertDialog(
            onDismissRequest = { showCurrencyDialog = false },
            title = { Text("Select Currency") },
            text = {
                Column {
                    currencies.forEach { currency ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setCurrency(currency)
                                    showCurrencyDialog = false
                                }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currency == currentCurrency,
                                onClick = null
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(currency)
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }
}

@Composable
fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = Color.Gray,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Column {
                content()
            }
        }
    }
}

@Composable
fun SettingsActionItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    indicatorColor: Color? = null,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (indicatorColor != null) {
                Box(
                    modifier = Modifier
                        .size(width = 4.dp, height = 24.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(indicatorColor)
                )
                Spacer(modifier = Modifier.width(12.dp))
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconTint.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                    color = titleColor,
                    fontSize = 15.sp
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

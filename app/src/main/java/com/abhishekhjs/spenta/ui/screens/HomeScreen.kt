package com.abhishekhjs.spenta.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.CircleShape
import coil.compose.AsyncImage
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.ui.res.stringResource
import com.abhishekhjs.spenta.ui.theme.Inter
import com.abhishekhjs.spenta.data.Transaction
import com.abhishekhjs.spenta.R
import com.abhishekhjs.spenta.data.TransactionViewModel
import com.abhishekhjs.spenta.ui.components.EditTransactionDialog
import com.abhishekhjs.spenta.ui.components.TransactionRow
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: TransactionViewModel,
    onNavigateToSplit: (String?) -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onAddTransaction: () -> Unit = {}
) {
    val transactions by viewModel.allTransactions.collectAsState()
    val categories by viewModel.allCategories.collectAsState()
    val initialBalance by viewModel.initialBalance.collectAsState()
    val currency by viewModel.currency.collectAsState()
    val userName by viewModel.userName.collectAsState()
    val profileImage by viewModel.profileImage.collectAsState()
    val budgetType by viewModel.budgetType.collectAsState()
    val budgetAmount by viewModel.budgetAmount.collectAsState()

    val isDark = isSystemInDarkTheme()

    var editingTransaction by remember { mutableStateOf<Transaction?>(null) }

    if (editingTransaction != null) {
        EditTransactionDialog(
            transaction = editingTransaction!!,
            categories = categories,
            onDismiss = { editingTransaction = null },
            onConfirm = { updated ->
                viewModel.update(updated)
                editingTransaction = null
            },
            onDelete = { toDelete ->
                viewModel.delete(toDelete)
                editingTransaction = null
            },
            onSplit = { toSplit ->
                editingTransaction = null
                onNavigateToSplit(toSplit.amount.toString())
            }
        )
    }

    val totalIncome = transactions.filter { it.type == "Income" && it.isPaid && it.isAcknowledged }.sumOf { it.amount }
    val totalExpense = transactions.filter { it.type == "Expense" && it.isPaid && it.isAcknowledged }.sumOf { it.amount }
    val currentBalance = initialBalance + totalIncome - totalExpense

    val todayTransactions = remember(transactions) {
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        val start = calendar.timeInMillis
        transactions.filter { it.timestamp >= start }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (userName.isEmpty()) "Hello Explorer" else "Hello, $userName",
                    style = MaterialTheme.typography.headlineMedium,
                    fontFamily = Inter,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Welcome back to Spenta",
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = Inter,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                if (profileImage.isNotEmpty()) {
                    AsyncImage(
                        model = profileImage,
                        contentDescription = "Profile Image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else {
                    Text(
                        text = if (userName.isNotEmpty()) userName.take(1).uppercase() else "S",
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = Inter,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))

        // New Balance Section
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Net Balance Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0xFF111111) else MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(32.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp, 
                    if (isDark) Color.White.copy(alpha = 0.05f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "NET BALANCE",
                        style = MaterialTheme.typography.labelLarge,
                        fontFamily = Inter,
                        fontWeight = FontWeight.Black,
                        color = if (isDark) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "$currency${String.format(Locale.US, "%,.2f", currentBalance)}",
                        style = MaterialTheme.typography.displayMedium,
                        fontFamily = Inter,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Box(
                        modifier = Modifier
                            .width(50.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary)
                    )
                }
            }

            // Income and Expense Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Total In Card
                BalanceMiniCard(
                    modifier = Modifier.weight(1f),
                    label = "TOTAL IN",
                    amount = "$currency${String.format(Locale.US, "%,.0f", totalIncome)}",
                    icon = Icons.Default.ArrowDownward,
                    color = if (isDark) Color(0xFF4CAF50) else Color(0xFF2E7D32),
                    isDark = isDark
                )

                // Total Out Card
                BalanceMiniCard(
                    modifier = Modifier.weight(1f),
                    label = "TOTAL OUT",
                    amount = "$currency${String.format(Locale.US, "%,.0f", totalExpense)}",
                    icon = Icons.Default.ArrowUpward,
                    color = if (isDark) Color(0xFFFF8A65) else Color(0xFFD84315),
                    isDark = isDark
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))

        // Quick Actions Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val buttonColors = ButtonDefaults.buttonColors(
                containerColor = if (isDark) Color(0xFF1E1E1E) else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (isDark) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = { onNavigateToSplit(null) },
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = buttonColors
            ) {
                Icon(Icons.Default.Groups, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.split_bill), fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            
            Button(
                onClick = { onNavigateToSettings() },
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = buttonColors
            ) {
                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.budget), fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        // Spending Status (Safe to Spend)
        if (budgetAmount > 0) {
            SpendingStatusCard(transactions, budgetType, budgetAmount, currency)
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No Budget Set",
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = Inter,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Set a budget to track your daily safe-to-spend limit.",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = Inter,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    Button(
                        onClick = onNavigateToSettings,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Get Started", fontFamily = Inter)
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Today's Activity",
            style = MaterialTheme.typography.titleLarge,
            fontFamily = Inter,
            fontWeight = FontWeight.Bold
        )
        
        // Placeholder for recent items
        Spacer(modifier = Modifier.height(16.dp))
        if (todayTransactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "No transactions today",
                        fontFamily = Inter,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onAddTransaction,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Add First Expense", fontFamily = Inter)
                    }
                }
            }
        } else {
            // Show today's transactions
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Prioritize unacknowledged transactions on Home
                val displayTransactions = todayTransactions.sortedWith(
                    compareByDescending<Transaction> { !it.isAcknowledged }
                    .thenByDescending { it.timestamp }
                )
                
                items(displayTransactions) { transaction ->
                    TransactionRow(
                        transaction = transaction,
                        isCompact = true,
                        currency = currency,
                        onLongClick = { editingTransaction = transaction },
                        onPayClick = { viewModel.update(transaction.copy(isPaid = true)) }
                    )
                }
            }
        }
    }
}

@Composable
fun BalanceMiniCard(
    modifier: Modifier = Modifier,
    label: String,
    amount: String,
    icon: ImageVector,
    color: Color,
    isDark: Boolean = isSystemInDarkTheme()
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF111111) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp, 
            if (isDark) Color.White.copy(alpha = 0.05f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = Inter,
                    fontWeight = FontWeight.Black,
                    color = if (isDark) Color.White.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.8.sp
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = amount,
                style = MaterialTheme.typography.headlineSmall,
                fontFamily = Inter,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
fun SpendingStatusCard(transactions: List<Transaction>, budgetType: String, budgetAmount: Double, currency: String) {
    val now = java.util.Calendar.getInstance()
    
    val todayStart = (now.clone() as java.util.Calendar).apply {
        set(java.util.Calendar.HOUR_OF_DAY, 0)
        set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
    }.timeInMillis

    val periodStart = (now.clone() as java.util.Calendar).apply {
        if (budgetType == "Weekly") {
            set(java.util.Calendar.DAY_OF_WEEK, firstDayOfWeek)
        } else {
            set(java.util.Calendar.DAY_OF_MONTH, 1)
        }
        set(java.util.Calendar.HOUR_OF_DAY, 0)
        set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
    }.timeInMillis

    val daysInPeriod = if (budgetType == "Weekly") 7 else now.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
    val currentDay = if (budgetType == "Weekly") {
        val firstDay = now.firstDayOfWeek
        (now.get(java.util.Calendar.DAY_OF_WEEK) - firstDay + 7) % 7 + 1
    } else {
        now.get(java.util.Calendar.DAY_OF_MONTH)
    }
    val daysRemaining = (daysInPeriod - currentDay + 1).coerceAtLeast(1)

    val periodExpenses = transactions.filter { it.type == "Expense" && it.timestamp >= periodStart }
    val spentBeforeToday = periodExpenses.filter { it.timestamp < todayStart }.sumOf { it.amount }
    val spentToday = periodExpenses.filter { it.timestamp >= todayStart }.sumOf { it.amount }
    
    val totalSpent = spentBeforeToday + spentToday
    val dailyAllowance = ((budgetAmount - spentBeforeToday) / daysRemaining).coerceAtLeast(0.0)
    val safeToSpendToday = (dailyAllowance - spentToday).coerceAtLeast(0.0)

    val percentUsed = (totalSpent / budgetAmount).coerceIn(0.0, 1.0)
    val isDark = isSystemInDarkTheme()
    val statusColor = when {
        percentUsed < 0.6 -> if (isDark) Color(0xFF4CAF50) else Color(0xFF2E7D32)
        percentUsed < 0.9 -> Color(0xFFFF9800)
        else -> MaterialTheme.colorScheme.error
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Safe to Spend Today",
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = Inter,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$currency${String.format(Locale.US, "%,.2f", safeToSpendToday)}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontFamily = Inter,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(statusColor.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (percentUsed < 0.9) Icons.Default.Fastfood else Icons.Default.ShoppingCart,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            LinearProgressIndicator(
                progress = { percentUsed.toFloat() },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = statusColor,
                trackColor = statusColor.copy(alpha = 0.1f)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Spent: $currency${String.format(Locale.US, "%,.0f", totalSpent)}",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = Inter,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Budget: $currency${String.format(Locale.US, "%,.0f", budgetAmount)}",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = Inter,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

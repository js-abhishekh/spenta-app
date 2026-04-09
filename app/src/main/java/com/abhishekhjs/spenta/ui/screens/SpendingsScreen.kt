package com.abhishekhjs.spenta.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abhishekhjs.spenta.data.Transaction
import com.abhishekhjs.spenta.data.TransactionViewModel
import com.abhishekhjs.spenta.ui.components.EditTransactionDialog
import com.abhishekhjs.spenta.ui.components.TransactionRow
import java.text.SimpleDateFormat
import java.util.*

enum class TimePeriod { Weekly, Monthly, Yearly, All }
enum class SortOption { Recent, Highest }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SpendingsScreen(viewModel: TransactionViewModel) {
    val allTransactions by viewModel.allTransactions.collectAsState(initial = emptyList())
    val categories by viewModel.allCategories.collectAsState()
    val currency by viewModel.currency.collectAsState()
    
    var selectedPeriod by remember { mutableStateOf(TimePeriod.Monthly) }
    var selectedSort by remember { mutableStateOf(SortOption.Recent) }
    var periodOffset by remember { mutableIntStateOf(0) }

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
            }
        )
    }

    // Reset offset when period changes
    LaunchedEffect(selectedPeriod) {
        periodOffset = 0
    }

    val periodInfo = remember(selectedPeriod, periodOffset) {
        getPeriodRange(selectedPeriod, periodOffset)
    }

    val filteredTransactions = remember(allTransactions, selectedPeriod, periodOffset, selectedSort) {
        val (start, end) = periodInfo
        
        val filtered = if (selectedPeriod == TimePeriod.All) {
            allTransactions
        } else {
            allTransactions.filter { it.timestamp in start..end }
        }

        filtered.sortedWith(compareByDescending<Transaction> { 
            if (selectedSort == SortOption.Recent) it.timestamp else it.amount 
        })
    }

    val categoryData = remember(filteredTransactions) {
        val data = mutableMapOf<String, Double>()
        filteredTransactions.forEach { trans ->
            val key = if (trans.type == "Income") "Income" else trans.category
            data[key] = (data[key] ?: 0.0) + trans.amount
        }
        data.toList().sortedByDescending { it.second }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Analytics",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Row {
                PeriodSelector(selectedPeriod) { selectedPeriod = it }
                SortSelector(selectedSort) { selectedSort = it }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Period Navigation (Hidden for "All")
        if (selectedPeriod != TimePeriod.All) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { periodOffset-- }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBackIos, contentDescription = "Prev", modifier = Modifier.size(16.dp))
                }
                
                Text(
                    text = getPeriodLabel(selectedPeriod, periodInfo.first),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                IconButton(onClick = { if (periodOffset < 0) periodOffset++ }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = "Next", modifier = Modifier.size(16.dp))
                }
            }
        } else {
            Text(
                text = "All Transactions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Bar Graph
        if (categoryData.isNotEmpty()) {
            SpendingBarGraph(categoryData, currency)
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Text("No data for this period", color = Color.Gray)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "History",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (filteredTransactions.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("No transactions found", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(filteredTransactions) { transaction ->
                    TransactionRow(
                        transaction = transaction,
                        currency = currency,
                        onLongClick = { editingTransaction = transaction }
                    )
                }
            }
        }
    }
}

@Composable
fun PeriodSelector(selected: TimePeriod, onSelect: (TimePeriod) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Default.FilterList, contentDescription = "Period")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            TimePeriod.entries.forEach { period ->
                DropdownMenuItem(
                    text = { Text(period.name) },
                    onClick = {
                        onSelect(period)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun SortSelector(selected: SortOption, onSelect: (SortOption) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Default.Sort, contentDescription = "Sort")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            SortOption.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(if (option == SortOption.Recent) "Most Recent" else "Highest Amount") },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun SpendingBarGraph(data: List<Pair<String, Double>>, currency: String) {
    val maxVal = data.maxOfOrNull { it.second }?.toFloat() ?: 0f
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            data.forEach { (category, amount) ->
                val ratio = if (maxVal > 0) (amount.toFloat() / maxVal) else 0f
                val color = if (category == "Income") Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
                
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = category, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        Text(text = "$currency${String.format(Locale.getDefault(), "%.0f", amount)}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Box(modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(Color.LightGray.copy(alpha = 0.2f))) {
                        Box(modifier = Modifier.fillMaxWidth(ratio).fillMaxHeight().clip(CircleShape).background(color))
                    }
                }
            }
        }
    }
}

private fun getPeriodRange(period: TimePeriod, offset: Int): Pair<Long, Long> {
    val cal = Calendar.getInstance()
    return when (period) {
        TimePeriod.Weekly -> {
            cal.add(Calendar.WEEK_OF_YEAR, offset)
            cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
            val start = cal.timeInMillis
            cal.add(Calendar.DAY_OF_YEAR, 6)
            val end = cal.timeInMillis
            start to end
        }
        TimePeriod.Monthly -> {
            cal.add(Calendar.MONTH, offset)
            cal.set(Calendar.DAY_OF_MONTH, 1)
            val start = cal.timeInMillis
            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
            val end = cal.timeInMillis
            start to end
        }
        TimePeriod.Yearly -> {
            cal.add(Calendar.YEAR, offset)
            cal.set(Calendar.DAY_OF_YEAR, 1)
            val start = cal.timeInMillis
            cal.set(Calendar.DAY_OF_YEAR, cal.getActualMaximum(Calendar.DAY_OF_YEAR))
            val end = cal.timeInMillis
            start to end
        }
        TimePeriod.All -> 0L to Long.MAX_VALUE
    }
}

private fun getPeriodLabel(period: TimePeriod, startTime: Long): String {
    val date = Date(startTime)
    return when (period) {
        TimePeriod.Weekly -> "Week of " + SimpleDateFormat("dd MMM", Locale.getDefault()).format(date)
        TimePeriod.Monthly -> SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(date)
        TimePeriod.Yearly -> SimpleDateFormat("yyyy", Locale.getDefault()).format(date)
        TimePeriod.All -> "All Time"
    }
}

package com.abhishekhjs.spenta.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abhishekhjs.spenta.ui.theme.Inter
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
fun SpendingsScreen(
    viewModel: TransactionViewModel,
    onAddTransaction: () -> Unit = {}
) {
    val allTransactions by viewModel.allTransactions.collectAsState(initial = emptyList())
    val categories by viewModel.allCategories.collectAsState()
    val currency by viewModel.currency.collectAsState()
    
    var selectedPeriod by remember { mutableStateOf(TimePeriod.Monthly) }
    var selectedSort by remember { mutableStateOf(SortOption.Recent) }
    var periodOffset by remember { mutableIntStateOf(0) }
    
    // Swipe gesture state
    var swipeOffsetX by remember { mutableStateOf(0f) }

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

    val expenseData = remember(filteredTransactions) {
        filteredTransactions.filter { it.type == "Expense" }
            .groupBy { 
                if (!it.isAcknowledged) "Unacknowledged"
                else if (it.category.isBlank()) "Uncategorized"
                else it.category 
            }
            .mapValues { it.value.sumOf { t -> t.amount } }
            .toList()
            .sortedByDescending { it.second }
    }

    val incomeData = remember(filteredTransactions) {
        filteredTransactions.filter { it.type == "Income" }
            .groupBy { 
                if (!it.isAcknowledged) "Unacknowledged"
                else if (it.category.isBlank()) "Uncategorized"
                else it.category 
            }
            .mapValues { it.value.sumOf { t -> t.amount } }
            .toList()
            .sortedByDescending { it.second }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .pointerInput(selectedPeriod) {
                if (selectedPeriod == TimePeriod.All) return@pointerInput
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (swipeOffsetX > 150) {
                            // Swipe Right -> Previous Period
                            periodOffset--
                        } else if (swipeOffsetX < -150) {
                            // Swipe Left -> Next Period
                            if (periodOffset < 0) periodOffset++
                        }
                        swipeOffsetX = 0f
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        swipeOffsetX += dragAmount
                    }
                )
            }
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
                fontFamily = Inter,
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
                    fontFamily = Inter,
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
                fontFamily = Inter,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Combined Scrollable Analytics and History
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Donut Charts Section
            if (expenseData.isNotEmpty() || incomeData.isNotEmpty()) {
                if (expenseData.isNotEmpty()) {
                    item {
                        SpendingDonutChart(expenseData, currency, "Expenses")
                    }
                }
                if (incomeData.isNotEmpty()) {
                    item {
                        SpendingDonutChart(incomeData, currency, "Income", isIncome = true)
                    }
                }
            } else {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No data for this period", fontFamily = Inter, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Transaction History Header
            item {
                Text(
                    text = "Transaction History",
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = Inter,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // Transaction History List
            if (filteredTransactions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "No transactions found",
                                fontFamily = Inter,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = onAddTransaction,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Add Transaction", fontFamily = Inter)
                            }
                        }
                    }
                }
            } else {
                val (pending, history) = filteredTransactions.partition { !it.isAcknowledged }

                if (pending.isNotEmpty()) {
                    item {
                        Text(
                            text = "Pending Review",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error,
                            fontFamily = Inter,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                    }
                    items(pending) { transaction ->
                        TransactionRow(
                            transaction = transaction,
                            currency = currency,
                            onLongClick = { editingTransaction = transaction },
                            onPayClick = { viewModel.update(transaction.copy(isPaid = true)) }
                        )
                    }
                    item {
                        Text(
                            text = "Past Transactions",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = Inter,
                            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                        )
                    }
                }
                
                items(history) { transaction ->
                    TransactionRow(
                        transaction = transaction,
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
fun PeriodSelector(selected: TimePeriod, onSelect: (TimePeriod) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Default.FilterList, contentDescription = "Period")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            TimePeriod.entries.forEach { period ->
                DropdownMenuItem(
                    text = { Text(period.name, fontFamily = Inter) },
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
                    text = { Text(if (option == SortOption.Recent) "Most Recent" else "Highest Amount", fontFamily = Inter) },
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
fun SpendingDonutChart(data: List<Pair<String, Double>>, currency: String, title: String, isIncome: Boolean = false) {
    val total = data.sumOf { it.second }
    if (total <= 0) return

    val expenseColors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
        Color(0xFF4DB6AC),
        Color(0xFFFF9800),
        Color(0xFFE91E63),
        Color(0xFF9C27B0),
        Color(0xFF00BCD4)
    )

    val incomeColors = listOf(
        Color(0xFF4CAF50), // Green
        Color(0xFF2196F3), // Blue
        Color(0xFF00BCD4), // Cyan
        Color(0xFF9C27B0), // Purple
        Color(0xFF009688), // Teal
        Color(0xFF8BC34A), // Lime
        Color(0xFF3F51B5), // Indigo
        Color(0xFF03A9F4)  // Light Blue
    )

    val colors = if (isIncome) incomeColors else expenseColors
    val isDark = isSystemInDarkTheme()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontFamily = Inter,
                fontWeight = FontWeight.ExtraBold,
                color = if (isIncome) {
                    if (isDark) Color(0xFF4CAF50) else Color(0xFF2E7D32)
                } else {
                    MaterialTheme.colorScheme.secondary
                },
                modifier = Modifier.padding(bottom = 20.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(160.dp)) {
                    var startAngle = -90f
                    data.forEachIndexed { index, (category, amount) ->
                        val sweepAngle = (amount / total * 360f).toFloat()
                        if (sweepAngle > 0.5f) {
                            val color = if (category == "Unacknowledged") Color(0xFFE57373) else colors[index % colors.size]
                            drawArc(
                                color = color,
                                startAngle = startAngle,
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                style = Stroke(width = 20.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                        startAngle += sweepAngle
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "TOTAL",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        fontFamily = Inter,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "$currency${String.format(Locale.getDefault(), "%,.0f", total)}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        fontFamily = Inter,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Improved Legibility Legend: Vertical list with soft backgrounds
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                data.take(5).forEachIndexed { index, (category, amount) ->
                    val color = if (category == "Unacknowledged") Color(0xFFE57373) else colors[index % colors.size]
                    val percentage = (amount / total * 100).toInt()
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = category,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                            fontFamily = Inter,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "$currency${String.format(Locale.getDefault(), "%,.0f", amount)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = Inter,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "$percentage%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontFamily = Inter
                            )
                        }
                    }
                }
            }

            if (data.size > 5) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "+ ${data.size - 5} MORE CATEGORIES",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    fontFamily = Inter
                )
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

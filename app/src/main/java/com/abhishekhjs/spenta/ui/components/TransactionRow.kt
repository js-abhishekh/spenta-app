package com.abhishekhjs.spenta.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abhishekhjs.spenta.data.Transaction
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun TransactionRow(
    transaction: Transaction,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false,
    currency: String = "$",
    onLongClick: () -> Unit = {}
) {
    val isExpense = transaction.type == "Expense"
    val dateFormat = if (isCompact) {
        SimpleDateFormat("MMM dd", Locale.getDefault())
    } else {
        SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    }
    val dateString = dateFormat.format(Date(transaction.timestamp))
    
    val icon = getCategoryIcon(transaction.category)

    val padding = if (isCompact) 12.dp else 16.dp
    val iconSize = if (isCompact) 16.dp else 20.dp
    val boxSize = if (isCompact) 32.dp else 40.dp
    val fontSizeMain = if (isCompact) 14.sp else 16.sp
    val fontSizeSub = if (isCompact) 10.sp else 12.sp

    Surface(
        color = if (transaction.isAcknowledged) MaterialTheme.colorScheme.surface 
                else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { if (!transaction.isAcknowledged) onLongClick() },
                onLongClick = onLongClick
            )
    ) {
        Row(
            modifier = Modifier
                .padding(padding)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(boxSize)
                    .clip(CircleShape)
                    .background(
                        if (isExpense) MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isExpense) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(iconSize)
                )
            }

            Spacer(modifier = Modifier.width(padding))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (!transaction.isAcknowledged && transaction.merchant.isEmpty()) "Unacknowledged" else transaction.merchant,
                    fontWeight = FontWeight.Bold,
                    fontSize = fontSizeMain,
                    color = if (transaction.isAcknowledged) MaterialTheme.colorScheme.onSurface 
                            else MaterialTheme.colorScheme.error
                )
                Text(
                    text = if (!transaction.isAcknowledged && transaction.category.isEmpty()) "Tap to identify" else transaction.category,
                    fontSize = fontSizeSub,
                    color = if (transaction.isAcknowledged) Color.Gray else MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${if (isExpense) "-" else "+"}$currency${String.format(Locale.US, "%.2f", transaction.amount)}",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = fontSizeMain,
                    color = if (isExpense) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                )
                Text(text = dateString, fontSize = fontSizeSub, color = Color.Gray)
            }
        }
    }
}

fun getCategoryIcon(category: String): ImageVector {
    return when (category) {
        "Food" -> Icons.Default.Fastfood
        "Shopping" -> Icons.Default.ShoppingCart
        "Work" -> Icons.Default.Work
        "Travel" -> Icons.Default.DirectionsCar
        "Health" -> Icons.Default.MedicalServices
        "Education" -> Icons.Default.School
        "Bills" -> Icons.Default.Receipt
        "Entertainment" -> Icons.Default.Movie
        "Income" -> Icons.Default.Payments
        else -> Icons.Default.Category
    }
}

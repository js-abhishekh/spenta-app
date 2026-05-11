package com.abhishekhjs.spenta.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.abhishekhjs.spenta.ui.theme.Inter
import com.abhishekhjs.spenta.data.Category
import com.abhishekhjs.spenta.data.Transaction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTransactionDialog(
    transaction: Transaction,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onConfirm: (Transaction) -> Unit,
    onDelete: (Transaction) -> Unit
) {
    var merchant by remember { mutableStateOf(transaction.merchant) }
    var amount by remember { mutableStateOf(transaction.amount.toString()) }
    var selectedCategory by remember { mutableStateOf(transaction.category) }
    var type by remember { mutableStateOf(transaction.type) }
    var categoryExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (!transaction.isAcknowledged) "Identify Transaction" else "Edit Transaction", fontFamily = Inter) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = type == "Expense",
                        onClick = { type = "Expense" },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) {
                        Text("Expense", fontFamily = Inter)
                    }
                    SegmentedButton(
                        selected = type == "Income",
                        onClick = { type = "Income" },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) {
                        Text("Income", fontFamily = Inter)
                    }
                }

                OutlinedTextField(
                    value = merchant,
                    onValueChange = { merchant = it },
                    label = { Text("Merchant", fontFamily = Inter) },
                    placeholder = { Text("Enter merchant name", fontFamily = Inter) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount", fontFamily = Inter) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = !categoryExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedCategory,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category", fontFamily = Inter) },
                        placeholder = { Text("Select category", fontFamily = Inter) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true).fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.name, fontFamily = Inter) },
                                onClick = {
                                    selectedCategory = category.name
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val updatedAmount = amount.toDoubleOrNull() ?: transaction.amount
                    onConfirm(transaction.copy(
                        merchant = merchant,
                        amount = updatedAmount,
                        category = selectedCategory,
                        type = type,
                        isAcknowledged = true
                    ))
                }
            ) {
                Text("Save", fontFamily = Inter)
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { onDelete(transaction) }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                    Text("Delete", fontFamily = Inter)
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel", fontFamily = Inter)
                }
            }
        }
    )
}

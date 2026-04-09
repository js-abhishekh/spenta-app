package com.abhishekhjs.spenta.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abhishekhjs.spenta.data.Category
import com.abhishekhjs.spenta.data.TransactionViewModel
import com.abhishekhjs.spenta.service.SpentaNotificationService
import com.abhishekhjs.spenta.ui.theme.CyberLime
import com.abhishekhjs.spenta.ui.theme.DeepOnyx
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    viewModel: TransactionViewModel,
    onFinish: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 5 })
    val scope = rememberCoroutineScope()

    Scaffold(
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Page Indicator
                Row {
                    repeat(5) { iteration ->
                        val color = if (pagerState.currentPage == iteration) CyberLime else Color.Gray
                        Box(
                            modifier = Modifier
                                .padding(4.dp)
                                .clip(CircleShape)
                                .background(color)
                                .size(8.dp)
                        )
                    }
                }

                Button(
                    onClick = {
                        if (pagerState.currentPage < 4) {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        } else {
                            onFinish()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberLime, contentColor = DeepOnyx)
                ) {
                    Text(if (pagerState.currentPage == 4) "Get Started" else "Next")
                }
            }
        },
        containerColor = DeepOnyx
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.padding(padding)
        ) { page ->
            when (page) {
                0 -> CurrencyStep(viewModel)
                1 -> BalanceStep(viewModel)
                2 -> PermissionsStep()
                3 -> CategoriesStep(viewModel)
                4 -> InteractiveDemoStep()
            }
        }
    }
}

@Composable
fun CurrencyStep(viewModel: TransactionViewModel) {
    val currentCurrency by viewModel.currency.collectAsState()
    val currencies = listOf("₹", "$", "€", "£", "¥", "₣")

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("Select Currency", fontSize = 32.sp, fontWeight = FontWeight.Black, color = CyberLime)
        Text("Choose your primary currency for tracking.", color = Color.Gray)
        Spacer(modifier = Modifier.height(32.dp))
        
        currencies.forEach { currency ->
            val isSelected = currency == currentCurrency
            Surface(
                onClick = { viewModel.setCurrency(currency) },
                color = if (isSelected) CyberLime.copy(alpha = 0.1f) else Color.Transparent,
                shape = RoundedCornerShape(12.dp),
                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, CyberLime) else null,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = isSelected, onClick = null, colors = RadioButtonDefaults.colors(selectedColor = CyberLime))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(currency, fontSize = 18.sp, color = if (isSelected) CyberLime else Color.White)
                }
            }
        }
    }
}

@Composable
fun BalanceStep(viewModel: TransactionViewModel) {
    val initialBalance by viewModel.initialBalance.collectAsState()
    var textValue by remember { mutableStateOf(if (initialBalance == 0.0) "" else initialBalance.toString()) }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("Wallet Balance", fontSize = 32.sp, fontWeight = FontWeight.Black, color = CyberLime)
        Text("Enter your current starting balance.", color = Color.Gray)
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = textValue,
            onValueChange = { 
                textValue = it
                it.toDoubleOrNull()?.let { b -> viewModel.setInitialBalance(b) }
            },
            label = { Text("Starting Balance") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CyberLime,
                focusedLabelColor = CyberLime,
                cursorColor = CyberLime,
                unfocusedTextColor = Color.White,
                focusedTextColor = Color.White
            )
        )
    }
}

@Composable
fun PermissionsStep() {
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("Permissions", fontSize = 32.sp, fontWeight = FontWeight.Black, color = CyberLime)
        Text("Spenta needs Notification Access to automatically detect expenses from bank SMS and alerts.", color = Color.Gray)
        Spacer(modifier = Modifier.height(32.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = CyberLime, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("Why this is safe?", fontWeight = FontWeight.Bold, color = Color.White)
                Text("We only process transaction-related keywords locally on your device. No data ever leaves your phone.", fontSize = 14.sp, color = Color.Gray)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = CyberLime, contentColor = DeepOnyx)
        ) {
            Text("Enable Notification Access")
        }
    }
}

@Composable
fun CategoriesStep(viewModel: TransactionViewModel) {
    val categories by viewModel.allCategories.collectAsState()
    var newCategoryName by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("Categories", fontSize = 32.sp, fontWeight = FontWeight.Black, color = CyberLime)
        Text("Manage your expense categories.", color = Color.Gray)
        Spacer(modifier = Modifier.height(24.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = newCategoryName,
                onValueChange = { newCategoryName = it },
                modifier = Modifier.weight(1f),
                label = { Text("Add New") },
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberLime, focusedLabelColor = CyberLime, focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )
            IconButton(
                onClick = {
                    if (newCategoryName.isNotBlank()) {
                        viewModel.insertCategory(Category(newCategoryName, "Category"))
                        newCategoryName = ""
                    }
                },
                modifier = Modifier.align(Alignment.CenterVertically).padding(start = 8.dp)
            ) {
                Icon(Icons.Default.AddCircle, contentDescription = null, tint = CyberLime)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(categories) { category ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(category.name, color = Color.White)
                    if (!category.isSystem) {
                        IconButton(onClick = { viewModel.deleteCategory(category) }) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red.copy(alpha = 0.6f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InteractiveDemoStep() {
    val context = LocalContext.current
    
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("Interactive Demo", fontSize = 32.sp, fontWeight = FontWeight.Black, color = CyberLime)
        Text("See how Spenta works with a real system notification.", color = Color.Gray)
        Spacer(modifier = Modifier.height(32.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Icon(Icons.Default.TouchApp, contentDescription = null, tint = CyberLime, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("Try Real Notification", fontWeight = FontWeight.Bold, color = Color.White)
                Text("Click the button below to trigger a test transaction notification. You'll see how you can enter the merchant and category directly from the notification shade.", fontSize = 14.sp, color = Color.Gray)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = {
                val intent = Intent(context, SpentaNotificationService::class.java).apply {
                    action = SpentaNotificationService.ACTION_TEST_NOTIFICATION
                }
                context.startService(intent)
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = CyberLime, contentColor = DeepOnyx)
        ) {
            Text("Send Test Notification")
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "Note: Make sure you've enabled Notification Access in the previous step.",
            fontSize = 12.sp,
            color = Color.Gray,
            lineHeight = 16.sp
        )
    }
}

package com.abhishekhjs.spenta.ui.screens

import android.content.Intent
import android.provider.Settings
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import coil.compose.AsyncImage
import com.abhishekhjs.spenta.data.Category
import com.abhishekhjs.spenta.data.TransactionViewModel
import com.abhishekhjs.spenta.isNotificationServiceEnabled
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
    val context = LocalContext.current
    val pagerState = rememberPagerState(pageCount = { 7 })
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
                    repeat(7) { iteration ->
                        val color = if (pagerState.currentPage == iteration) MaterialTheme.colorScheme.primary else Color.Gray
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
                        val currentPage = pagerState.currentPage
                        if (currentPage < 6) {
                            val budgetAmount = viewModel.budgetAmount.value
                            val isPermissionGranted = isNotificationServiceEnabled(context)
                            
                            when (currentPage) {
                                3 -> {
                                    if (budgetAmount <= 0) {
                                        Toast.makeText(context, "Please set a budget amount to continue", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                }
                                4 -> {
                                    if (!isPermissionGranted) {
                                        Toast.makeText(context, "Please enable Notification Access to continue", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                }
                            }
                            
                            scope.launch { pagerState.animateScrollToPage(currentPage + 1) }
                        } else {
                            onFinish()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
                ) {
                    Text(if (pagerState.currentPage == 6) "Get Started" else "Next")
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.padding(padding)
        ) { page ->
            when (page) {
                0 -> ProfileStep(viewModel)
                1 -> CurrencyStep(viewModel)
                2 -> BalanceStep(viewModel)
                3 -> BudgetStep(viewModel)
                4 -> PermissionsStep()
                5 -> CategoriesStep(viewModel)
                6 -> InteractiveDemoStep()
            }
        }
    }
}

@Composable
fun ProfileStep(viewModel: TransactionViewModel) {
    val userName by viewModel.userName.collectAsState()
    val profileImage by viewModel.profileImage.collectAsState()
    
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            viewModel.setProfileImage(it.toString())
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Create Profile", fontSize = 32.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary, modifier = Modifier.align(Alignment.Start))
        Text("Personalize your Spenta experience.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.align(Alignment.Start))
        Spacer(modifier = Modifier.height(48.dp))

        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                .clickable { launcher.launch("image/*") },
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
                Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(40.dp))
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = userName,
            onValueChange = { 
                viewModel.setUserName(it)
            },
            label = { Text("Your Name") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                cursorColor = MaterialTheme.colorScheme.primary,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedTextColor = MaterialTheme.colorScheme.onSurface
            )
        )
    }
}

@Composable
fun CurrencyStep(viewModel: TransactionViewModel) {
    val currentCurrency by viewModel.currency.collectAsState()
    val currencies = listOf("₹", "$", "€", "£", "¥", "₣")

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("Select Currency", fontSize = 32.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
        Text("Choose your primary currency for tracking.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(32.dp))
        
        currencies.forEach { currency ->
            val isSelected = currency == currentCurrency
            Surface(
                onClick = { viewModel.setCurrency(currency) },
                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent,
                shape = RoundedCornerShape(12.dp),
                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = isSelected, onClick = null, colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(currency, fontSize = 18.sp, color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
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
        Text("Wallet Balance", fontSize = 32.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
        Text("Enter your current starting balance.", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                cursorColor = MaterialTheme.colorScheme.primary,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedTextColor = MaterialTheme.colorScheme.onSurface
            )
        )
    }
}

@Composable
fun BudgetStep(viewModel: TransactionViewModel) {
    val budgetType by viewModel.budgetType.collectAsState()
    val budgetAmount by viewModel.budgetAmount.collectAsState()
    var textValue by remember { mutableStateOf(if (budgetAmount == 0.0) "" else budgetAmount.toString()) }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("Set Budget", fontSize = 32.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
        Text("Choose your budget period and limit.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(32.dp))

        Text("Budget Type", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            listOf("Weekly", "Monthly").forEach { type ->
                val isSelected = type == budgetType
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.setBudgetType(type) },
                    label = { Text(type) },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = textValue,
            onValueChange = { 
                textValue = it
                it.toDoubleOrNull()?.let { b -> viewModel.setBudgetAmount(b) }
            },
            label = { Text("Budget Limit") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                cursorColor = MaterialTheme.colorScheme.primary,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedTextColor = MaterialTheme.colorScheme.onSurface
            )
        )
    }
}

@Composable
fun PermissionsStep() {
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("Permissions", fontSize = 32.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
        Text("Spenta needs Notification Access to automatically detect expenses from bank SMS and alerts.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(32.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("Why this is safe?", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text("We only process transaction-related keywords locally on your device. No data ever leaves your phone.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
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
        Text("Categories", fontSize = 32.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
        Text("Manage your expense categories.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(24.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = newCategoryName,
                onValueChange = { newCategoryName = it },
                modifier = Modifier.weight(1f),
                label = { Text("Add New") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                )
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
                Icon(Icons.Default.AddCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
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
                    Text(category.name, color = MaterialTheme.colorScheme.onSurface)
                    if (!category.isSystem) {
                        IconButton(onClick = { viewModel.deleteCategory(category) }) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
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
        Text("Interactive Demo", fontSize = 32.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
        Text("See how Spenta works with a real system notification.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(32.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Icon(Icons.Default.TouchApp, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("Try Real Notification", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text("Click the button below to trigger a test transaction notification. You'll see how you can enter the merchant and category directly from the notification shade.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
        ) {
            Text("Send Test Notification")
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "Note: Make sure you've enabled Notification Access in the previous step.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 16.sp
        )
    }
}

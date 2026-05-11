package com.abhishekhjs.spenta.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.*
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import androidx.compose.ui.window.Dialog
import com.abhishekhjs.spenta.data.Transaction
import com.abhishekhjs.spenta.data.TransactionViewModel
import com.abhishekhjs.spenta.nearby.NearbyManager
import com.abhishekhjs.spenta.ui.components.QRCodeGenerator
import com.abhishekhjs.spenta.ui.components.QRCodeScanner
import com.abhishekhjs.spenta.ui.theme.Inter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitBillScreen(
    viewModel: TransactionViewModel,
    nearbyManager: NearbyManager,
    amount: String?,
    merchant: String?,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val discoveredEndpoints by nearbyManager.discoveredEndpoints.collectAsState()
    val connectedEndpoints by nearbyManager.connectedEndpoints.collectAsState()

    val permissionsNeeded = remember {
        val list = mutableListOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            list.add(Manifest.permission.BLUETOOTH_SCAN)
            list.add(Manifest.permission.BLUETOOTH_ADVERTISE)
            list.add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }
        list
    }

    var hasPermissions by remember {
        mutableStateOf(
            permissionsNeeded.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasPermissions = results.values.all { it }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "scanning")
    val scanningAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scanningAlpha"
    )
    
    // Fix: Handle "null" or placeholder strings from navigation arguments
    var totalAmount by remember(amount) { 
        val clean = if (amount == null || amount == "null" || amount.startsWith("{")) "" else amount
        mutableStateOf(clean) 
    }
    var description by remember(merchant) { 
        val clean = if (merchant == null || merchant == "null" || merchant.startsWith("{")) "" else merchant
        mutableStateOf(clean) 
    }

    var selectedFriends by remember { mutableStateOf(setOf<String>()) }
    var splitType by remember { mutableStateOf("Equally") } // "Equally" or "Exact"
    val specificAmounts = remember { mutableStateMapOf<String, String>() }

    var showQRDialog by remember { mutableStateOf(false) }
    var showScannerDialog by remember { mutableStateOf(false) }

    val isDark = isSystemInDarkTheme()

    val currencySymbol = viewModel.preferenceManager.getCurrency()

    DisposableEffect(hasPermissions) {
        if (hasPermissions) {
            nearbyManager.startDiscovery()
            val advertisingName = "${viewModel.preferenceManager.getUserName()}|${viewModel.preferenceManager.getProfileImage()}"
            nearbyManager.startAdvertising(advertisingName)
        }
        onDispose {
            nearbyManager.stopAll()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Split Bill",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontFamily = Inter
                )
                Text(
                    text = "Divide with friends",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = Inter
                )
            }

            IconButton(
                onClick = { showScannerDialog = true },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
            ) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan", tint = MaterialTheme.colorScheme.primary)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Amount and Description Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) Color(0xFF111111) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
            ),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    "TOTAL AMOUNT", 
                    color = if (isDark) Color.White.copy(alpha = 0.6f) else MaterialTheme.colorScheme.primary,
                    fontSize = 12.sp, 
                    fontWeight = FontWeight.Black, 
                    fontFamily = Inter,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        currencySymbol, 
                        color = if (isDark) Color.White.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.4f),
                        fontSize = 32.sp, 
                        fontWeight = FontWeight.Bold,
                        fontFamily = Inter,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    androidx.compose.foundation.text.BasicTextField(
                        value = totalAmount,
                        onValueChange = { input ->
                            if (input.isEmpty() || input.all { char -> char.isDigit() || char == '.' || char == ',' }) {
                                val clean = input.replace(',', '.')
                                if (clean.count { it == '.' } <= 1) {
                                    val parts = clean.split('.')
                                    if (parts.size < 2 || parts[1].length <= 2) {
                                        totalAmount = clean
                                    }
                                }
                            }
                        },
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontFamily = Inter,
                            color = if (isDark) Color.White else MaterialTheme.colorScheme.onPrimaryContainer,
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Start
                        ),
                        modifier = Modifier.padding(start = 12.dp).weight(1f),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                        ),
                        decorationBox = { innerTextField ->
                            Box(contentAlignment = Alignment.CenterStart) {
                                if (totalAmount.isEmpty()) {
                                    Text(
                                        text = "0.00",
                                        color = if (isDark) Color.White.copy(alpha = 0.1f) else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f),
                                        fontSize = 48.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = Inter
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                TextField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = { 
                        Text(
                            "What's this for?", 
                            color = if (isDark) Color.White.copy(alpha = 0.3f) else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.3f), 
                            fontFamily = Inter
                        ) 
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = if (isDark) Color.White.copy(alpha = 0.05f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                        unfocusedContainerColor = if (isDark) Color.White.copy(alpha = 0.05f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.3f),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedTextColor = if (isDark) Color.White else MaterialTheme.colorScheme.onPrimaryContainer,
                        unfocusedTextColor = if (isDark) Color.White else MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    textStyle = androidx.compose.ui.text.TextStyle(fontFamily = Inter, textAlign = TextAlign.Start),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Nearby Explorers Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("NEARBY EXPLORERS", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = Inter)
            if (hasPermissions) {
                Text(
                    text = "SCANNING...",
                    color = MaterialTheme.colorScheme.primary.copy(alpha = scanningAlpha),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = Inter
                )
            } else {
                Text(
                    text = "GRANT PERMISSION",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = Inter,
                    modifier = Modifier.clickable { launcher.launch(permissionsNeeded.toTypedArray()) }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(discoveredEndpoints) { endpoint ->
                val isConnected = connectedEndpoints.any { it.id == endpoint.id }
                ExplorerItem(
                    name = endpoint.name,
                    status = if (isConnected) "Found" else "Searching...",
                    isSelected = selectedFriends.contains(endpoint.id),
                    isConnected = isConnected,
                    onClick = {
                        if (!isConnected) {
                            nearbyManager.connectToEndpoint(endpoint.id, viewModel.preferenceManager.getUserName())
                        } else {
                            selectedFriends = if (selectedFriends.contains(endpoint.id)) {
                                selectedFriends - endpoint.id
                            } else {
                                selectedFriends + endpoint.id
                            }
                        }
                    }
                )
            }
            
            item {
                InviteItem(onClick = { showQRDialog = true })
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Split Control and List Section
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(32.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                // Custom Tab Switcher
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(4.dp)
                ) {
                    val tabModifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(12.dp))

                    Box(
                        modifier = tabModifier
                            .background(if (splitType == "Equally") MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { splitType = "Equally" },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "EQUALLY", 
                            color = if (splitType == "Equally") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, 
                            fontWeight = FontWeight.Black, 
                            fontSize = 12.sp,
                            fontFamily = Inter,
                            letterSpacing = 1.sp
                        )
                    }
                    Box(
                        modifier = tabModifier
                            .background(if (splitType == "Exact") MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { 
                                if (splitType != "Exact") {
                                    val equalShare = calculateAmount(totalAmount, selectedFriends.size + 1, "Equally", "", emptyMap(), emptySet())
                                    selectedFriends.forEach { id ->
                                        specificAmounts[id] = equalShare
                                    }
                                    splitType = "Exact"
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "EXACT", 
                            color = if (splitType == "Exact") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, 
                            fontWeight = FontWeight.Black, 
                            fontSize = 12.sp,
                            fontFamily = Inter,
                            letterSpacing = 1.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // Current User
                    item {
                        val userName = viewModel.preferenceManager.getUserName()
                        val profileImage = viewModel.preferenceManager.getProfileImage()
                        val myAmount = calculateAmount(totalAmount, selectedFriends.size + 1, splitType, "me", specificAmounts, selectedFriends)
                        ParticipantItem(
                            name = "You",
                            subtext = userName,
                            amount = myAmount,
                            currencySymbol = currencySymbol,
                            isMe = true,
                            profileImage = profileImage
                        )
                    }

                    items(selectedFriends.toList()) { id ->
                        val friend = connectedEndpoints.find { it.id == id }
                        val amountVal = calculateAmount(totalAmount, selectedFriends.size + 1, splitType, id, specificAmounts, selectedFriends)
                        ParticipantItem(
                            name = friend?.name ?: "Unknown",
                            subtext = "Connected",
                            amount = amountVal,
                            currencySymbol = currencySymbol,
                            profileImage = friend?.profileImage ?: "",
                            onAmountChange = if (splitType == "Exact") { { input ->
                                if (input.isEmpty() || input.all { char -> char.isDigit() || char == '.' || char == ',' }) {
                                    val clean = input.replace(',', '.')
                                    if (clean.count { it == '.' } <= 1) {
                                        val parts = clean.split('.')
                                        if (parts.size < 2 || parts[1].length <= 2) {
                                            specificAmounts[id] = clean
                                        }
                                    }
                                }
                            } } else null
                        )
                    }
                }

                val totalVal = totalAmount.toDoubleOrNull() ?: 0.0
                val friendsSum = selectedFriends.sumOf { specificAmounts[it]?.toDoubleOrNull() ?: 0.0 }
                if (splitType == "Exact" && friendsSum > totalVal) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    ) {
                        Text(
                            text = "Sum exceeds total amount!",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = Inter,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Shared with ${selectedFriends.size + 1} people", 
                        color = MaterialTheme.colorScheme.onSurfaceVariant, 
                        fontSize = 12.sp, 
                        fontFamily = Inter,
                        fontWeight = FontWeight.Medium
                    )
                    // Avatars stack
                    Row(horizontalArrangement = Arrangement.spacedBy((-8).dp)) {
                        repeat(minOf(selectedFriends.size + 1, 4)) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val amountVal = totalAmount.toDoubleOrNull() ?: 0.0
                val myName = viewModel.preferenceManager.getUserName()
                if (amountVal > 0 && selectedFriends.isNotEmpty()) {
                    val mySplitAmount = calculateAmount(totalAmount, selectedFriends.size + 1, splitType, "me", specificAmounts, selectedFriends).toDoubleOrNull() ?: 0.0
                    
                    // Add sender's share to their transaction list
                    if (mySplitAmount > 0) {
                        viewModel.insert(
                            Transaction(
                                amount = mySplitAmount,
                                merchant = description.ifEmpty { "Split Bill" },
                                category = "Bills", 
                                type = "Expense",
                                timestamp = System.currentTimeMillis()
                            )
                        )
                    }

                    selectedFriends.forEach { id ->
                        val splitAmount = calculateAmount(totalAmount, selectedFriends.size + 1, splitType, id, specificAmounts, selectedFriends).toDoubleOrNull() ?: 0.0
                        if (splitAmount > 0) {
                            val payload = "SPLIT|$splitAmount|${description.ifEmpty { "Group Expense" }}|$myName"
                            nearbyManager.sendPayload(id, payload)
                        }
                    }
                    onBack()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary, 
                contentColor = MaterialTheme.colorScheme.onPrimary,
                disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(20.dp),
            enabled = selectedFriends.isNotEmpty() && (totalAmount.toDoubleOrNull() ?: 0.0) > 0
        ) {
            Text("SEND SPLIT REQUEST", fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp, fontFamily = Inter)
        }

        if (showQRDialog) {
            Dialog(onDismissRequest = { showQRDialog = false }) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Your Split ID", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 18.sp, fontFamily = Inter)
                        Spacer(modifier = Modifier.height(16.dp))
                        QRCodeGenerator(content = "SPENTA_ID|${viewModel.preferenceManager.getUserName()}")
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Ask your friend to scan this QR", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, fontFamily = Inter)
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { showQRDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
                        ) {
                            Text("Close", fontFamily = Inter)
                        }
                    }
                }
            }
        }

        if (showScannerDialog) {
            Dialog(onDismissRequest = { showScannerDialog = false }) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth().height(450.dp)
                ) {
                    Column {
                        Box(modifier = Modifier.weight(1f)) {
                            QRCodeScanner { result ->
                                if (result.startsWith("SPENTA_ID|")) {
                                    val friendName = result.removePrefix("SPENTA_ID|")
                                    discoveredEndpoints.find { it.name == friendName }?.let { endpoint ->
                                        nearbyManager.connectToEndpoint(endpoint.id, viewModel.preferenceManager.getUserName())
                                    }
                                    showScannerDialog = false
                                }
                            }
                        }
                        Button(
                            onClick = { showScannerDialog = false },
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
                        ) {
                            Text("Cancel", fontFamily = Inter)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExplorerItem(name: String, status: String, isSelected: Boolean, isConnected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(100.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .border(
                if (isSelected) 1.dp else 0.dp,
                if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Text(name, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, fontFamily = Inter)
        Text(status, color = if (isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontFamily = Inter)
    }
}

@Composable
fun InviteItem(onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .width(100.dp)
            .height(54.dp)
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Text("Invite", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontFamily = Inter)
    }
}

@Composable
fun ParticipantItem(
    name: String,
    subtext: String,
    amount: String,
    currencySymbol: String,
    isMe: Boolean = false,
    profileImage: String = "",
    onAmountChange: ((String) -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (isMe) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            if (isMe && profileImage.isEmpty()) {
                Text(name.take(2).uppercase(), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black, fontFamily = Inter)
            } else if (profileImage.isNotEmpty()) {
                AsyncImage(
                    model = profileImage,
                    contentDescription = "Profile Image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            } else {
                Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(name, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp, fontFamily = Inter)
            Text(subtext, color = if (isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontFamily = Inter)
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(currencySymbol, color = if (isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = Inter)
            if (onAmountChange != null) {
                androidx.compose.foundation.text.BasicTextField(
                    value = amount,
                    onValueChange = onAmountChange,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontFamily = Inter,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End
                    ),
                    modifier = Modifier.width(80.dp),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                    ),
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.CenterEnd) {
                            if (amount.isEmpty()) Text("0.00", color = MaterialTheme.colorScheme.onSurfaceVariant, fontFamily = Inter)
                            innerTextField()
                        }
                    }
                )
            } else {
                Text(
                    text = amount,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = Inter,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    }
}

fun calculateAmount(total: String, count: Int, type: String, id: String, specific: Map<String, String>, selectedIds: Set<String>): String {
    val locale = java.util.Locale.US
    if (total.isEmpty()) return "0.00"
    val totalVal = total.toDoubleOrNull() ?: 0.0
    if (totalVal <= 0.0) return "0.00"
    
    return if (type == "Equally") {
        if (count <= 0) "0.00" 
        else {
            // Truncate friends' share to 2 decimal places to ensure sum(others) <= totalVal
            val shareVal = kotlin.math.floor((totalVal / count) * 100.0) / 100.0
            if (id == "me") {
                val othersSum = shareVal * (count - 1)
                val remaining = totalVal - othersSum
                String.format(locale, "%.2f", if (remaining < 0) 0.0 else remaining)
            } else {
                String.format(locale, "%.2f", shareVal)
            }
        }
    } else {
        if (id == "me") {
            val friendsSum = selectedIds.sumOf { 
                val v = specific[it]?.toDoubleOrNull() ?: 0.0
                // Format to 2 decimal places to be consistent with what's displayed and sent
                String.format(locale, "%.2f", v).toDouble()
            }
            val remaining = totalVal - friendsSum
            // Round to handle floating point precision errors
            val roundedRemaining = kotlin.math.round(remaining * 100.0) / 100.0
            String.format(locale, "%.2f", if (roundedRemaining < 0) 0.0 else roundedRemaining)
        } else {
            val amtVal = specific[id]?.toDoubleOrNull() ?: 0.0
            String.format(locale, "%.2f", amtVal)
        }
    }
}

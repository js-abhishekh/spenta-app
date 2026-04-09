package com.abhishekhjs.spenta.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abhishekhjs.spenta.data.Category
import com.abhishekhjs.spenta.data.TransactionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategorySettingsScreen(viewModel: TransactionViewModel, onBack: () -> Unit) {
    val categories by viewModel.allCategories.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Custom Categories") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Category")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(categories) { category ->
                CategoryItem(category) {
                    if (!category.isSystem) {
                        viewModel.deleteCategory(category)
                    }
                }
            }
        }

        if (showAddDialog) {
            AddCategoryDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { name, icon ->
                    viewModel.insertCategory(Category(name, icon))
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun CategoryItem(category: Category, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getIconForName(category.iconName),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = category.name,
                modifier = Modifier.weight(1f),
                fontWeight = FontWeight.Medium
            )
            if (!category.isSystem) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray)
                }
            } else {
                Text("System", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
        }
    }
}

@Composable
fun AddCategoryDialog(onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf("Category") }
    
    val icons = listOf(
        "Fastfood", "ShoppingCart", "Work", "DirectionsCar", 
        "Home", "School", "Favorite", "FitnessCenter",
        "LocalAtm", "Flight", "Restaurant", "Movie",
        "Build", "Brush", "Pets", "Healing",
        "Lightbulb", "SelfImprovement", "Commute", "Store"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Category") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Category Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Choose Icon", style = MaterialTheme.typography.labelLarge)
                
                // Show icons in a grid
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    icons.chunked(5).forEach { rowIcons ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            rowIcons.forEach { iconName ->
                                IconButton(
                                    onClick = { selectedIcon = iconName },
                                    modifier = Modifier.background(
                                        if (selectedIcon == iconName) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                        else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                ) {
                                    Icon(getIconForName(iconName), contentDescription = null)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { if (name.isNotBlank()) onConfirm(name, selectedIcon) }) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

fun getIconForName(name: String) = when (name) {
    "Fastfood" -> Icons.Default.Fastfood
    "ShoppingCart" -> Icons.Default.ShoppingCart
    "Work" -> Icons.Default.Work
    "DirectionsCar" -> Icons.Default.DirectionsCar
    "Home" -> Icons.Default.Home
    "School" -> Icons.Default.School
    "Favorite" -> Icons.Default.Favorite
    "FitnessCenter" -> Icons.Default.FitnessCenter
    "LocalAtm" -> Icons.Default.LocalAtm
    "Flight" -> Icons.Default.Flight
    "Restaurant" -> Icons.Default.Restaurant
    "Movie" -> Icons.Default.Movie
    "Build" -> Icons.Default.Build
    "Brush" -> Icons.Default.Brush
    "Pets" -> Icons.Default.Pets
    "Healing" -> Icons.Default.Healing
    "Lightbulb" -> Icons.Default.Lightbulb
    "SelfImprovement" -> Icons.Default.SelfImprovement
    "Commute" -> Icons.Default.Commute
    "Store" -> Icons.Default.Store
    else -> Icons.Default.Category
}

package com.cookbook.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.cookbook.data.model.PantryItem
import com.cookbook.ui.theme.*
import com.cookbook.ui.viewmodel.CookbookViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantryScreen(
    viewModel: CookbookViewModel,
    onBack: () -> Unit,
    onNavigateToMainMenu: () -> Unit,
    onNavigateToChat: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var showSideMenu by remember { mutableStateOf(false) }
    var showBudgetDialog by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<PantryItem?>(null) }
    var itemToDelete by remember { mutableStateOf<PantryItem?>(null) }
    var selectedTab by remember { mutableIntStateOf(1) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.refreshPantry()
    }

    var currentPage by remember { mutableIntStateOf(1) }
    val itemsPerPage = 6

    val filteredItems = remember(state.pantryItems, searchQuery) {
        if (searchQuery.isBlank()) state.pantryItems
        else state.pantryItems.filter {
            it.name.contains(searchQuery, ignoreCase = true)
        }
    }

    val totalPages = remember(filteredItems) {
        maxOf(1, kotlin.math.ceil(filteredItems.size / itemsPerPage.toDouble()).toInt())
    }

    LaunchedEffect(totalPages) {
        if (currentPage > totalPages) {
            currentPage = totalPages
        }
    }

    val paginatedItems = remember(filteredItems, currentPage) {
        val startIndex = (currentPage - 1) * itemsPerPage
        filteredItems.drop(startIndex).take(itemsPerPage)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(CreamLight)
        ) {
            // Top bar
            CenterAlignedTopAppBar(
                modifier = Modifier
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(GreenDark, GreenPrimary)
                        )
                    ),
                title = {
                    Text(
                        text = "Dirk's CookBook",
                        style = MaterialTheme.typography.titleMedium,
                        color = White,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { showSideMenu = true }) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu", tint = White)
                    }
                },
                actions = {
                    IconButton(onClick = { showBudgetDialog = true }) {
                        Icon(Icons.Default.MonetizationOn, contentDescription = "Budget", tint = White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )

            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search pantry...", color = LightGray) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = MediumGray)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GreenPrimary,
                    unfocusedBorderColor = LightGray
                )
            )

            // Pantry grid
            if (paginatedItems.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Kitchen,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = LightGray
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Your pantry is empty",
                            style = MaterialTheme.typography.titleMedium,
                            color = MediumGray
                        )
                        Text(
                            "Tap + to add items",
                            style = MaterialTheme.typography.bodySmall,
                            color = LightGray
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 12.dp),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(paginatedItems) { item ->
                        PantryCard(
                            item = item,
                            onClick = { itemToEdit = item },
                            onDeleteClick = { itemToDelete = item }
                        )
                    }
                }
                
                if (totalPages > 1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val startPage = maxOf(1, minOf(currentPage - 2, totalPages - 4))
                        val endPage = minOf(totalPages, startPage + 4)
                        for (page in startPage..endPage) {
                            val isSelected = page == currentPage
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) GreenPrimary else Color.Transparent)
                                    .border(1.dp, if (isSelected) GreenPrimary else LightGray, CircleShape)
                                    .clickable { currentPage = page },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = page.toString(),
                                    color = if (isSelected) White else DarkGray,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }

            BottomNavBar(selectedTab = selectedTab, onTabSelected = { tab ->
                when (tab) {
                    0 -> onNavigateToMainMenu()
                    1 -> { }
                    2 -> onNavigateToChat()
                }
            })
        }

        // FAB
        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 100.dp),
            containerColor = GreenPrimary,
            contentColor = White,
            shape = CircleShape
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add item", modifier = Modifier.size(28.dp))
        }

        // Side menu
        if (showSideMenu) {
            SideMenuOverlay(
                viewModel = viewModel,
                onDismiss = { showSideMenu = false },
                onChangePassword = { },
                onLogout = { }
            )
        }

        // Budget dialog
        if (showBudgetDialog) {
            AddBudgetDialog(viewModel = viewModel, onDismiss = { showBudgetDialog = false })
        }
    }

    // Add item dialog
    if (showAddDialog) {
        AddPantryItemDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { name, qty, expDate ->
                viewModel.addPantryItem(name, qty, expDate)
                showAddDialog = false
            }
        )
    }

    // Edit item dialog
    itemToEdit?.let { item ->
        EditPantryItemDialog(
            item = item,
            onDismiss = { itemToEdit = null },
            onSave = { newName, newQty, newExpDate ->
                viewModel.editPantryItem(item, newName, newQty, newExpDate)
                itemToEdit = null
            }
        )
    }

    // Delete item dialog
    itemToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("Delete Item", textAlign = TextAlign.Center) },
            text = { Text("Are you sure you want to delete ${item.name}?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deletePantryItem(item)
                    itemToDelete = null
                }) {
                    Text("Yes", color = ErrorRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("No")
                }
            }
        )
    }
}

@Composable
fun PantryCard(item: PantryItem, onClick: () -> Unit = {}, onDeleteClick: () -> Unit = {}) {
    val status = remember(item.expDate) { getExpiryStatus(item.expDate) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(GreenPrimary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Egg,
                    contentDescription = null,
                    tint = GreenPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }

            Text(
                text = item.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = DarkGray,
                textAlign = TextAlign.Center
            )

            if (item.qty.isNotBlank()) {
                Text(
                    text = item.qty,
                    style = MaterialTheme.typography.bodySmall,
                    color = MediumGray
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            when (status) {
                                ExpiryStatus.FRESH -> SuccessGreen
                                ExpiryStatus.EXPIRING -> WarningOrange
                                ExpiryStatus.EXPIRED -> ErrorRed
                            }
                        )
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = when (status) {
                        ExpiryStatus.FRESH -> "Fresh"
                        ExpiryStatus.EXPIRING -> "Expiring"
                        ExpiryStatus.EXPIRED -> "Expired"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = when (status) {
                        ExpiryStatus.FRESH -> SuccessGreen
                        ExpiryStatus.EXPIRING -> WarningOrange
                        ExpiryStatus.EXPIRED -> ErrorRed
                    }
                )
            } // ends Row
        } // ends Column
        IconButton(
            onClick = onDeleteClick,
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Delete",
                tint = ErrorRed,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
}

enum class ExpiryStatus { FRESH, EXPIRING, EXPIRED }

private fun getExpiryStatus(expDate: String): ExpiryStatus {
    if (expDate.isBlank()) return ExpiryStatus.FRESH
    return try {
        val date = LocalDate.parse(expDate, DateTimeFormatter.ofPattern("MM/dd/yyyy"))
        val daysUntilExpiry = ChronoUnit.DAYS.between(LocalDate.now(), date)
        when {
            daysUntilExpiry < 0 -> ExpiryStatus.EXPIRED
            daysUntilExpiry <= 3 -> ExpiryStatus.EXPIRING
            else -> ExpiryStatus.FRESH
        }
    } catch (_: Exception) {
        ExpiryStatus.FRESH
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPantryItemDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, qty: String, expDate: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var qty by remember { mutableStateOf("") }
    var expDate by remember { mutableStateOf("") }
    var dateError by remember { mutableStateOf(false) }
    var emptyError by remember { mutableStateOf(false) }
    var zeroQtyError by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Item to Pantry", textAlign = TextAlign.Center) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { 
                        name = it
                        emptyError = false
                    },
                    label = { Text("Food Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    isError = emptyError && name.isBlank()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = qty,
                    onValueChange = { 
                        qty = it
                        emptyError = false
                        zeroQtyError = false
                    },
                    label = { Text("Quantity") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    isError = (emptyError && qty.isBlank()) || zeroQtyError
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = expDate,
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Expiry Date (MM/DD/YYYY)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        isError = dateError || (emptyError && expDate.isBlank())
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { showDatePicker = true }
                    )
                }
                if (emptyError) {
                    Text("All fields must be filled.", color = ErrorRed, fontSize = 12.sp)
                }
                if (zeroQtyError) {
                    Text("Quantity must be a valid positive number.", color = ErrorRed, fontSize = 12.sp)
                }
                if (dateError) {
                    Text("Invalid date format. Use MM/DD/YYYY",
                        color = ErrorRed, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isBlank() || qty.isBlank() || expDate.isBlank()) {
                    emptyError = true
                    return@TextButton
                }
                val qtyInt = qty.toIntOrNull()
                if (qtyInt == null || qtyInt <= 0) {
                    zeroQtyError = true
                    return@TextButton
                }
                if (expDate.isNotBlank()) {
                    try {
                        LocalDate.parse(expDate, DateTimeFormatter.ofPattern("MM/dd/yyyy"))
                    } catch (_: Exception) {
                        dateError = true
                        return@TextButton
                    }
                }
                onAdd(name, qty, expDate)
            }) {
                Text("Add to Pantry", color = GreenPrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = java.time.Instant.ofEpochMilli(millis).atZone(java.time.ZoneId.of("UTC")).toLocalDate()
                        expDate = date.format(DateTimeFormatter.ofPattern("MM/dd/yyyy"))
                        dateError = false
                        emptyError = false
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPantryItemDialog(
    item: PantryItem,
    onDismiss: () -> Unit,
    onSave: (name: String, qty: String, expDate: String) -> Unit
) {
    var name by remember { mutableStateOf(item.name) }
    var qty by remember { mutableStateOf(item.qty) }
    var expDate by remember { mutableStateOf(item.expDate) }
    var dateError by remember { mutableStateOf(false) }
    var emptyError by remember { mutableStateOf(false) }
    var zeroQtyError by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Item", textAlign = TextAlign.Center) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { 
                        name = it
                        emptyError = false
                    },
                    label = { Text("Food Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    isError = emptyError && name.isBlank()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = qty,
                    onValueChange = { 
                        qty = it
                        emptyError = false
                        zeroQtyError = false
                    },
                    label = { Text("Quantity") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    isError = (emptyError && qty.isBlank()) || zeroQtyError
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = expDate,
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Expiry Date (MM/DD/YYYY)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        isError = dateError || (emptyError && expDate.isBlank())
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { showDatePicker = true }
                    )
                }
                if (emptyError) {
                    Text("All fields must be filled.", color = ErrorRed, fontSize = 12.sp)
                }
                if (zeroQtyError) {
                    Text("Quantity must be a valid positive number.", color = ErrorRed, fontSize = 12.sp)
                }
                if (dateError) {
                    Text("Invalid date format. Use MM/DD/YYYY", color = ErrorRed, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isBlank() || qty.isBlank() || expDate.isBlank()) {
                    emptyError = true
                    return@TextButton
                }
                val qtyInt = qty.toIntOrNull()
                if (qtyInt == null || qtyInt <= 0) {
                    zeroQtyError = true
                    return@TextButton
                }
                if (expDate.isNotBlank()) {
                    try {
                        LocalDate.parse(expDate, DateTimeFormatter.ofPattern("MM/dd/yyyy"))
                    } catch (_: Exception) {
                        dateError = true
                        return@TextButton
                    }
                }
                onSave(name, qty, expDate)
            }) {
                Text("Save", color = GreenPrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = java.time.Instant.ofEpochMilli(millis).atZone(java.time.ZoneId.of("UTC")).toLocalDate()
                        expDate = date.format(DateTimeFormatter.ofPattern("MM/dd/yyyy"))
                        dateError = false
                        emptyError = false
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

package com.cookbook.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cookbook.data.model.PantryItem
import com.cookbook.ui.theme.*
import com.cookbook.ui.viewmodel.CookbookViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainMenuScreen(
    viewModel: CookbookViewModel,
    onNavigateToChat: () -> Unit,
    onNavigateToPantry: () -> Unit,
    onChangePassword: () -> Unit,
    onLogout: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    var showSideMenu by remember { mutableStateOf(false) }
    var showBudgetDialog by remember { mutableStateOf(false) }
    var showShoppingList by remember { mutableStateOf(false) }
    var showTrashConfirm by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }

    val allChecked = state.checkedIngredients.isNotEmpty() &&
            state.checkedIngredients.all { it }

    LaunchedEffect(state.toastMessage) {
        if (state.toastMessage != null) {
            kotlinx.coroutines.delay(2000)
            viewModel.clearToast()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(GreenDark, GreenPrimary, GreenLight)
                    )
                )
        )

        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar
            CenterAlignedTopAppBar(
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

            // Main Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                // Generate Meal Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = TransparentWhite)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Restaurant,
                            contentDescription = null,
                            tint = GreenPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Generate Your Next Meal",
                            style = MaterialTheme.typography.titleMedium,
                            color = DarkGray,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Let AI decide.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MediumGray
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                val prompt = viewModel.generateFromPantry()
                                if (prompt != null) {
                                    onNavigateToChat()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            shape = RoundedCornerShape(22.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                        ) {
                            Icon(Icons.Default.Kitchen, contentDescription = null,
                                modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Generate from My Pantry",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Shopping List Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = TransparentWhite)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Shopping List",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = DarkGray
                            )
                            if (state.currentRecipeName != "No meal selected") {
                                IconButton(onClick = { showTrashConfirm = true }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Trash",
                                        tint = ErrorRed)
                                }
                            }
                        }

                        Text(
                            text = state.currentRecipeName,
                            style = MaterialTheme.typography.titleSmall,
                            color = GreenPrimary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Budget: ${state.currentBudget}", style = MaterialTheme.typography.bodySmall,
                                color = MediumGray)
                            Text(
                                text = "Est. Cost: Php %.2f".format(state.currentTotalCost),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (state.currentTotalCost > parseBudgetAmount(state.currentBudget) && !state.isFromPantry)
                                    ErrorRed else MediumGray
                            )
                        }

                        Text(
                            text = "Calories: ${state.currentCalories} | Protein: ${state.currentProtein}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MediumGray,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        Text(
                            text = state.savedMissingIngredients,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (state.savedMissingIngredients.contains("None")) SuccessGreen else WarningOrange,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )

                        // Ingredient checklist
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(horizontal = 8.dp),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            itemsIndexed(state.currentIngredients) { index, ingredient ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.toggleIngredientCheck(
                                                index,
                                                !state.checkedIngredients.getOrElse(index) { false }
                                            )
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = state.checkedIngredients.getOrElse(index) { false },
                                        onCheckedChange = {
                                            viewModel.toggleIngredientCheck(index, it)
                                        },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = GreenPrimary,
                                            uncheckedColor = LightGray
                                        )
                                    )
                                    Text(
                                        text = ingredient,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = DarkGray,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        // Done button
                        if (state.currentIngredients.isNotEmpty()) {
                            Button(
                                onClick = {
                                    viewModel.completeShoppingList()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                                    .height(44.dp),
                                shape = RoundedCornerShape(22.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                                enabled = allChecked
                            ) {
                                Text("Done", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Bottom Navigation
            BottomNavBar(
                selectedTab = selectedTab,
                onTabSelected = { tab ->
                    when (tab) {
                        0 -> { } // Already on Home
                        1 -> onNavigateToPantry()
                        2 -> onNavigateToChat()
                    }
                    selectedTab = tab
                }
            )
        }

        // Toast
        if (state.toastMessage != null) {
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 100.dp, start = 32.dp, end = 32.dp),
                containerColor = SuccessGreen
            ) {
                Text(state.toastMessage!!, color = White)
            }
        }
    }

    // Side Menu Drawer
    if (showSideMenu) {
        SideMenuOverlay(
            viewModel = viewModel,
            onDismiss = { showSideMenu = false },
            onChangePassword = {
                showSideMenu = false
                onChangePassword()
            },
            onLogout = {
                showSideMenu = false
                onLogout()
            },
            onDeleteAccount = {
                showSideMenu = false
                viewModel.deleteAccount { success, errorMsg ->
                    if (success) {
                        onLogout()
                    } else if (errorMsg != null) {
                        // Show error message
                    }
                }
            }
        )
    }

    // Budget Dialog
    if (showBudgetDialog) {
        AddBudgetDialog(
            viewModel = viewModel,
            onDismiss = { showBudgetDialog = false }
        )
    }

    // Shopping List Sheet
    if (showShoppingList) {
        ShoppingListSheet(
            viewModel = viewModel,
            onDismiss = { showShoppingList = false }
        )
    }

    // Trash Confirm
    if (showTrashConfirm) {
        AlertDialog(
            onDismissRequest = { showTrashConfirm = false },
            title = { Text("Trash Recipe") },
            text = { Text("Are you sure you want to trash this recipe?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearRecipe()
                    showTrashConfirm = false
                }) {
                    Text("Yes", color = ErrorRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTrashConfirm = false }) {
                    Text("No")
                }
            }
        )
    }
}

private fun parseBudgetAmount(budget: String): Double {
    return try {
        budget.replace("Php", "").replace(",", "").trim().toDouble()
    } catch (_: Exception) {
        0.0
    }
}

@Composable
fun BottomNavBar(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(White)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        NavTabItem(icon = Icons.Default.Home, label = "Home", selected = selectedTab == 0) {
            onTabSelected(0)
        }
        NavTabItem(icon = Icons.Default.Kitchen, label = "My Pantry", selected = selectedTab == 1) {
            onTabSelected(1)
        }
        NavTabItem(icon = Icons.Default.Chat, label = "AI Chat", selected = selectedTab == 2) {
            onTabSelected(2)
        }
    }
}

@Composable
fun NavTabItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(if (selected) GreenPrimary.copy(alpha = 0.15f) else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = if (selected) GreenPrimary else NavInactiveGray,
                modifier = Modifier.size(24.dp)
            )
        }
        Text(
            text = label,
            fontSize = 11.sp,
            color = if (selected) GreenPrimary else NavInactiveGray,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
fun SideMenuOverlay(
    viewModel: CookbookViewModel,
    onDismiss: () -> Unit,
    onChangePassword: () -> Unit,
    onLogout: () -> Unit,
    onDeleteAccount: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    var showEditProfile by remember { mutableStateOf(false) }
    var showHelp by remember { mutableStateOf(false) }
    var showLogoutConfirm by remember { mutableStateOf(false) }
    var showDeleteAccount by remember {mutableStateOf(false)}

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(OverlayBlack)
                .clickable { onDismiss() }
        )

        Column(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.72f)
                .background(White)
                .clickable(enabled = false) {}
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            Column(
                modifier = Modifier.padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Profile", style = MaterialTheme.typography.labelSmall,
                    color = MediumGray, modifier = Modifier.fillMaxWidth())

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(GreenPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = viewModel.state.value.firstName.first().uppercase(),
                        color = White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "${state.firstName} ${state.lastName}".trim(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = DarkGray
                )
                Text(
                    text = state.userEmail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MediumGray
                )

                Spacer(modifier = Modifier.height(12.dp))
            
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 16.dp, horizontal = 24.dp),
                color = LightGray
            )

            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                Text("Settings & Support", style = MaterialTheme.typography.labelSmall,
                    color = MediumGray)

                Spacer(modifier = Modifier.height(8.dp))

                SideMenuItem(Icons.Default.Lock, "Change Password") { onChangePassword() }
                SideMenuItem(Icons.Default.Logout, "Logout") { showLogoutConfirm = true }
                SideMenuItem(Icons.Default.Help, "Help & FAQs") { showHelp = true }
                SideMenuItem(Icons.Default.Delete, "Delete Account"){showDeleteAccount = true}
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "Back To Home Page",
                style = MaterialTheme.typography.labelLarge,
                color = GreenPrimary,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onDismiss() }
                    .padding(16.dp),
                textAlign = TextAlign.Center
            )
        }
    }

    if (showEditProfile) {
        EditProfileDialog(
            viewModel = viewModel,
            onDismiss = { showEditProfile = false }
        )
    }

    if (showHelp) {
        AlertDialog(
            onDismissRequest = { showHelp = false },
            title = { Text("Help & FAQs") },
            text = { Text("For support, contact us at:\nyjac2005@gmail.com") },
            confirmButton = {
                TextButton(onClick = { showHelp = false }) {
                    Text("Close", color = GreenPrimary)
                }
            }
        )
    }

    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text("Logout") },
            text = { Text("Are you sure you want to log out?") },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutConfirm = false
                    onLogout()
                }) {
                    Text("Yes", color = ErrorRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) {
                    Text("No")
                }
            }
        )
    }

    if (showDeleteAccount) {
        AlertDialog(
            onDismissRequest = { showDeleteAccount = false },
            title = { Text("Delete Account") },
            text = { Text("Are you sure you want to permanently delete your account? This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteAccount = false
                    onDeleteAccount()
                }) {
                    Text("Delete", color = ErrorRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAccount = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SideMenuItem(icon: ImageVector, title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = DarkGray, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(title, style = MaterialTheme.typography.bodyLarge, color = DarkGray,
            modifier = Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, contentDescription = null,
            tint = LightGray, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun EditProfileDialog(
    viewModel: CookbookViewModel,
    onDismiss: () -> Unit
) {
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Profile", textAlign = TextAlign.Center) },
        text = {
            Column {
                OutlinedTextField(
                    value = firstName,
                    onValueChange = { firstName = it },
                    label = { Text("First Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = lastName,
                    onValueChange = { lastName = it },
                    label = { Text("Last Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                com.cookbook.backend.UserProfileBackend.updateProfile(firstName, lastName)
                onDismiss()
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
}

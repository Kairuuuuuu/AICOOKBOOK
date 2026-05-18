package com.cookbook.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cookbook.backend.AIChatBot
import com.cookbook.backend.FirebaseManager
import com.cookbook.data.model.ParsedResponse
import com.cookbook.ui.theme.*
import com.cookbook.ui.viewmodel.CookbookViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val isThinking: Boolean = false,
    val recipe: ParsedResponse? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: CookbookViewModel,
    onBack: () -> Unit,
    onNavigateToMainMenu: () -> Unit,
    onNavigateToPantry: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    var inputText by remember { mutableStateOf("") }
    var isThinking by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var showBudgetWarning by remember { mutableStateOf(false) }
    var warningMessage by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(2) }
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    var showSideMenu by remember { mutableStateOf(false) }
    var showBudgetDialog by remember { mutableStateOf(false) }
    var showHistorySidebar by remember { mutableStateOf(false) }

    // Chat sessions list for sidebar
    var chatSessions by remember { mutableStateOf(listOf<Map<String, Any>>()) }
    var chatToDelete by remember { mutableStateOf<String?>(null) }

    val initialGreeting = "Hello! I'm Chef Dirk, your personal AI chef. " +
            "Ask me to suggest a recipe or tell me what ingredients you have!"

    val pastChats = listOf(
        "Adobo with 500 budget",
        "Pantry check: eggs, flour",
        "Healthy breakfast ideas",
        "Chicken soup recipe",
        "Garlic Price Breakdown Per Piece",
        "AI Responses to Inappropriate Input"
    )

    suspend fun sendMessage(
        text: String,
        currentMessages: List<ChatMessage>,
        setThinking: (Boolean) -> Unit,
        scrollState: androidx.compose.foundation.lazy.LazyListState
    ) {
        val updated = currentMessages + ChatMessage(text, isUser = true)
        viewModel.activeChatMessages.clear()
        viewModel.activeChatMessages.addAll(updated)

        setThinking(true)
        viewModel.activeChatMessages.add(ChatMessage("", isUser = false, isThinking = true))

        val result = withContext(Dispatchers.IO) {
            AIChatBot.askChefAI(text, viewModel.state.value.currentBudget)
        }

        setThinking(false)
        // Remove the thinking bubble
        if (viewModel.activeChatMessages.isNotEmpty() && viewModel.activeChatMessages.last().isThinking) {
            viewModel.activeChatMessages.removeAt(viewModel.activeChatMessages.lastIndex)
        }

        val finalMessages = if (result.hasRecipe) {
            updated + ChatMessage(
                "Here's what I found: **${result.recipeName}**\n\n" +
                        result.ingredients.joinToString("\n") + "\n\n" +
                        "Estimated cost: Php %.2f\nCalories: ${result.calories}\nProtein: ${result.protein}"
                            .format(result.totalEstimatedCost),
                isUser = false,
                recipe = result
            )
        } else {
            updated + ChatMessage(
                result.recipeName.ifBlank { "I couldn't find a recipe for that. Try something else!" },
                isUser = false
            )
        }

        viewModel.activeChatMessages.clear()
        viewModel.activeChatMessages.addAll(finalMessages)

        if (result.hasRecipe) {
            viewModel.activeAiResponse = result
        }

        // Save Chat Session to Firebase
        var activeChatId = viewModel.activeChatId
        if (activeChatId.isEmpty()) {
            activeChatId = java.util.UUID.randomUUID().toString()
            viewModel.activeChatId = activeChatId
        }

        val chatTitle = finalMessages.firstOrNull { it.isUser }?.text?.take(25)?.let { "$it..." } ?: "New Chat"
        val messagesData = finalMessages.map {
            val map = mutableMapOf<String, Any>(
                "text" to it.text,
                "isUser" to it.isUser
            )
            if (it.recipe != null) {
                map["hasRecipe"] = true
                map["recipeName"] = it.recipe.recipeName
                map["ingredients"] = it.recipe.ingredients
                map["totalEstimatedCost"] = it.recipe.totalEstimatedCost
                map["calories"] = it.recipe.calories
                map["protein"] = it.recipe.protein
            }
            map
        }

        FirebaseManager.saveChatSession(activeChatId, chatTitle, messagesData)

        FirebaseManager.loadChatSessions { sessions ->
            chatSessions = sessions
        }

        kotlinx.coroutines.delay(100)
        if (viewModel.activeChatMessages.isNotEmpty()) {
            scrollState.animateScrollToItem(viewModel.activeChatMessages.size - 1)
        }
    }

    LaunchedEffect(Unit) {
        // If it's completely empty, initialize it with the greeting
        if (viewModel.activeChatMessages.isEmpty()) {
            viewModel.activeChatMessages.add(ChatMessage(initialGreeting, isUser = false))
        }
        if (state.pendingPantryPrompt != null) {
            inputText = state.pendingPantryPrompt!!
            scope.launch {
                sendMessage(
                    viewModel.state.value.pendingPantryPrompt!!,
                    viewModel.activeChatMessages,
                    { isThinking = it },
                    listState
                )
            }
        }
        FirebaseManager.loadChatSessions { sessions ->
            chatSessions = sessions
        }
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
                        androidx.compose.ui.graphics.Brush.horizontalGradient(
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
                    IconButton(onClick = { showHistorySidebar = true }) {
                        Icon(Icons.Default.History, contentDescription = "Chat History", tint = White)
                    }
                    IconButton(onClick = { showBudgetDialog = true }) {
                        Icon(Icons.Default.MonetizationOn, contentDescription = "Budget", tint = White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )

            // Chat history
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                state = listState,
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(viewModel.activeChatMessages) { message ->
                    ChatBubble(message = message, onAddToCart = { recipe ->
                        viewModel.activeAiResponse = recipe
                        val analysis = com.cookbook.backend.Chatbackend.analyzeRecipe(
                            recipe, state.currentBudget)
                        when (analysis.status) {
                            com.cookbook.data.model.BudgetStatus.NO_BUDGET -> {
                                warningMessage = "You haven't set a budget. Want to add to the shopping list anyway?"
                                showBudgetWarning = true
                            }
                            com.cookbook.data.model.BudgetStatus.INSUFFICIENT_FUNDS -> {
                                warningMessage = "This recipe costs Php %.2f but your budget is Php %.2f. Add anyway?"
                                    .format(analysis.finalOutOfPocketCost, analysis.currentBudget)
                                showBudgetWarning = true
                            }
                            com.cookbook.data.model.BudgetStatus.OK -> {
                                showConfirmDialog = true
                            }
                        }
                    })
                }
            }

            // Input bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp,
                color = White
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Type a message...", color = LightGray) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (inputText.isNotBlank() && !isThinking) {
                                    val text = inputText.trim()
                                    inputText = ""
                                    focusManager.clearFocus()
                                    scope.launch {
                                        sendMessage(text, viewModel.activeChatMessages, { isThinking = it }, listState)
                                    }
                                }
                            }
                        ),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GreenPrimary,
                            unfocusedBorderColor = LightGray
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank() && !isThinking) {
                                val text = inputText.trim()
                                inputText = ""
                                focusManager.clearFocus()
                                scope.launch {
                                    sendMessage(text, viewModel.activeChatMessages, { isThinking = it }, listState)
                                }
                            }
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(GreenPrimary),
                        enabled = inputText.isNotBlank() && !isThinking
                    ) {
                        Icon(
                            Icons.Default.Send,
                            contentDescription = "Send",
                            tint = White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            BottomNavBar(selectedTab = selectedTab, onTabSelected = { tab ->
                when (tab) {
                    0 -> onNavigateToMainMenu()
                    1 -> onNavigateToPantry()
                    2 -> { }
                }
            })
        }



        // Side menu drawer
        if (showSideMenu) {
            SideMenuOverlay(
                viewModel = viewModel,
                onDismiss = { showSideMenu = false },
                onChangePassword = { },
                onLogout = { },
                onDeleteAccount = { }
            )
        }

        // Budget dialog
        if (showBudgetDialog) {
            AddBudgetDialog(viewModel = viewModel, onDismiss = { showBudgetDialog = false })
        }

        // --- ANIMATED CHAT HISTORY SIDEBAR OVERLAY ---
        AnimatedVisibility(
            visible = showHistorySidebar,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { showHistorySidebar = false }
            )
        }

        AnimatedVisibility(
            visible = showHistorySidebar,
            enter = slideInHorizontally(initialOffsetX = { fullWidth -> fullWidth }),
            exit = slideOutHorizontally(targetOffsetX = { fullWidth -> fullWidth }),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            val highlightCol = GreenPrimary
            val inactiveTextCol = DarkGray

            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(300.dp)
                    .background(White)
                    .clickable(enabled = false) {}
                    .padding(top = 56.dp, bottom = 16.dp)
            ) {
                // "New chat" row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            // Triggers the ViewModel session reset logic
                            viewModel.startNewChatSession(ChatMessage(initialGreeting, isUser = false))
                            showHistorySidebar = false
                        }
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New Chat",
                        tint = Black,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "New chat",
                        color = Black,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // "Chats" Header
                Text(
                    text = "Chats",
                    color = GreenDark,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )

                // Render history
                LazyColumn(
                    modifier = Modifier.weight(1f)
                ) {
                    itemsIndexed(chatSessions) { index, session ->
                        val sessionId = session["id"] as? String ?: ""
                        val chatTitle = session["title"] as? String ?: "Chat"
                        val isSelected = sessionId == viewModel.activeChatId

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 2.dp)
                                .clip(RoundedCornerShape(50))
                                .background(if (isSelected) highlightCol else Color.Transparent)
                                .clickable {
                                    viewModel.activeChatId = sessionId
                                    val rawMessages = session["messages"] as? List<*> ?: emptyList<Any>()

                                    val parsed = rawMessages.mapNotNull {
                                        val map = it as? Map<*, *>
                                        if (map != null) {
                                            val hasRecipe = map["hasRecipe"] as? Boolean ?: false
                                            val recipe = if (hasRecipe) {
                                                ParsedResponse(
                                                    recipeName = map["recipeName"] as? String ?: "",
                                                    ingredients = (map["ingredients"] as? List<*>)?.mapNotNull { item -> item as? String } ?: emptyList(),
                                                    hasRecipe = true,
                                                    totalEstimatedCost = (map["totalEstimatedCost"] as? Number)?.toDouble() ?: 0.0,
                                                    calories = map["calories"] as? String ?: "N/A",
                                                    protein = map["protein"] as? String ?: "N/A"
                                                )
                                            } else null
                                            ChatMessage(
                                                text = map["text"] as? String ?: "",
                                                isUser = map["isUser"] as? Boolean ?: false,
                                                isThinking = false,
                                                recipe = recipe
                                            )
                                        } else null
                                    }

                                    viewModel.activeChatMessages.clear()
                                    viewModel.activeChatMessages.addAll(parsed)
                                    viewModel.activeAiResponse = null
                                    showHistorySidebar = false
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = chatTitle,
                                color = if (isSelected) Color.White else inactiveTextCol,
                                fontSize = 14.sp,
                                maxLines = 1,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                modifier = Modifier.weight(1f).padding(end = 8.dp)
                            )
                            IconButton(
                                onClick = { chatToDelete = sessionId },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = if (isSelected) Color.White else ErrorRed,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Confirm add to shopping list
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Add to Shopping List") },
            text = { Text("Do you want to add missing ingredients to the shopping list?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.activeAiResponse?.let { viewModel.saveRecipeToMenu(it) }
                    showConfirmDialog = false
                    viewModel.activeAiResponse = null
                }) {
                    Text("Yes", color = GreenPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("No")
                }
            }
        )
    }

    // Budget warning dialog
    if (showBudgetWarning) {
        AlertDialog(
            onDismissRequest = { showBudgetWarning = false },
            title = { Text("Budget Warning") },
            text = { Text(warningMessage) },
            confirmButton = {
                TextButton(onClick = {
                    showBudgetWarning = false
                    showConfirmDialog = true
                }) {
                    Text("Add Anyway", color = WarningOrange)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBudgetWarning = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete chat dialog
    chatToDelete?.let { chatId ->
        val chatTitleToDelete = chatSessions.find { it["id"] as? String == chatId }?.get("title") as? String ?: "this chat"
        AlertDialog(
            onDismissRequest = { chatToDelete = null },
            title = { Text("Delete Chat", textAlign = androidx.compose.ui.text.style.TextAlign.Center) },
            text = { Text("Are you sure you want to delete $chatTitleToDelete?") },
            confirmButton = {
                TextButton(onClick = {
                    com.cookbook.backend.FirebaseManager.deleteChatSession(chatId) {
                        com.cookbook.backend.FirebaseManager.loadChatSessions { sessions ->
                            chatSessions = sessions
                        }
                        if (viewModel.activeChatId == chatId) {
                            viewModel.startNewChatSession(ChatMessage(initialGreeting, isUser = false))
                        }
                    }
                    chatToDelete = null
                }) {
                    Text("Yes", color = ErrorRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { chatToDelete = null }) {
                    Text("No")
                }
            }
        )
    }
}

@Composable
fun ChatBubble(message: ChatMessage, onAddToCart: ((ParsedResponse) -> Unit)? = null) {
    val alignment = if (message.isUser) Alignment.End else Alignment.Start
    val bgColor = if (message.isUser) ChatUserBubble else ChatAiBubble
    val textColor = if (message.isUser) White else DarkGray

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 280.dp),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (message.isUser) 16.dp else 4.dp,
                bottomEnd = if (message.isUser) 4.dp else 16.dp
            ),
            color = bgColor
        ) {
            if (message.isThinking) {
                Row(modifier = Modifier.padding(12.dp)) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = GreenPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("thinking...", color = MediumGray, fontSize = 13.sp)
                }
            } else {
                Text(
                    text = message.text,
                    modifier = Modifier.padding(12.dp),
                    color = textColor,
                    fontSize = 14.sp
                )
            }
        }
        
        if (message.recipe != null && onAddToCart != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.widthIn(max = 280.dp),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(
                    onClick = { onAddToCart(message.recipe) },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(GreenPrimary)
                ) {
                    Icon(
                        Icons.Default.ShoppingCart,
                        contentDescription = "Add to list",
                        tint = White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
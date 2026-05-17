package com.cookbook.ui.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cookbook.backend.AIChatBot
import com.cookbook.backend.AuthenticationService
import com.cookbook.backend.BudgetService
import com.cookbook.backend.ChangePasswordService
import com.cookbook.backend.Chatbackend
import com.cookbook.backend.EmailAuthenticationService
import com.cookbook.backend.FirebaseManager
import com.cookbook.backend.ForgotPasswordBackend
import com.cookbook.backend.PantryBackend
import com.cookbook.backend.ShoppingListBackend
import com.cookbook.backend.SignUpBackend
import com.cookbook.backend.UserProfileBackend
import com.cookbook.backend.VerificationBackend
import com.cookbook.data.model.*
import com.cookbook.ui.screens.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CookbookViewModel : ViewModel() {

    private val _state = MutableStateFlow(CookbookState())
    val state: StateFlow<CookbookState> = _state.asStateFlow()

    // --- NEW: Persistent Active Chat State Variables ---
    val activeChatMessages = mutableStateListOf<ChatMessage>()
    var activeChatId by mutableStateOf("")
    var activeAiResponse by mutableStateOf<ParsedResponse?>(null)

    // Call this when resetting or starting a clean session
    fun startNewChatSession(initialGreeting: ChatMessage) {
        activeChatMessages.clear()
        activeChatMessages.add(initialGreeting)
        activeChatId = ""
        activeAiResponse = null
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }

    fun clearToast() {
        _state.update { it.copy(toastMessage = null) }
    }

    private fun syncToCloud() {
        FirebaseManager.saveUserDataToCloud(
            _state.value.currentBudget,
            PantryBackend.savedPantryItems,
            _state.value.currentRecipeName
        )
    }

    fun login(email: String, password: String) {
        _state.update { it.copy(isLoading = true, errorMessage = null) }
        AuthenticationService.attemptLogin(
            email = email,
            password = password,
            onResult = { resultEmail, firstName ->
                UserProfileBackend.email = resultEmail
                UserProfileBackend.firstName = firstName
                FirebaseManager.loadUserDataFromCloud { savedBudget, savedPantry, savedRecipe ->
                    PantryBackend.savedPantryItems.clear()
                    PantryBackend.savedPantryItems.addAll(savedPantry)
                    _state.update {
                        it.copy(
                            isLoading = false,
                            userEmail = resultEmail,
                            firstName = firstName,
                            currentBudget = savedBudget,
                            pantryItems = savedPantry,
                            currentRecipeName = savedRecipe
                        )
                    }
                }
            },
            onError = { errorMsg ->
                _state.update { it.copy(isLoading = false, errorMessage = errorMsg) }
            }
        )
    }

    fun signUp(email: String, password: String, confirmPassword: String) {
        _state.update { it.copy(isLoading = true, errorMessage = null) }
        SignUpBackend.attemptSignUp(
            email = email,
            password = password,
            confirmPassword = confirmPassword,
            onSuccess = {
                _state.update { it.copy(isLoading = false, userEmail = email, signUpSuccess = true) }
            },
            onError = { errorMsg ->
                _state.update { it.copy(isLoading = false, errorMessage = errorMsg) }
            }
        )
    }

    fun changePassword(email: String, currentPassword: String, newPassword: String, confirmPassword: String) {
        _state.update { it.copy(isLoading = true, errorMessage = null) }
        ChangePasswordService.updatePassword(
            email = email,
            currentPassword = currentPassword,
            newPassword = newPassword,
            confirmPassword = confirmPassword,
            onSuccess = {
                _state.update { it.copy(isLoading = false, passwordChangeSuccess = true) }
            },
            onError = { errorMsg ->
                _state.update { it.copy(isLoading = false, errorMessage = errorMsg) }
            }
        )
    }

    fun forgotPasswordChange(email: String, newPassword: String, confirmPassword: String) {
        _state.update { it.copy(isLoading = true, errorMessage = null) }
        ForgotPasswordBackend.processPasswordChange(
            email = email,
            newPassword = newPassword,
            confirmPassword = confirmPassword,
            onSuccess = {
                _state.update { it.copy(isLoading = false, passwordChangeSuccess = true) }
            },
            onError = { errorMsg ->
                _state.update { it.copy(isLoading = false, errorMessage = errorMsg) }
            }
        )
    }

    fun logout() {
        UserProfileBackend.performLogout()
        _state.update { CookbookState() }
        activeChatMessages.clear()
        activeChatId = ""
        activeAiResponse = null
    }

    suspend fun sendOTP(email: String): OTPResult {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val result = EmailAuthenticationService.processEmailForOTP(email)
            if (result.status == OTPStatus.SUCCESS) {
                _state.update { it.copy(userEmail = email, errorMessage = null) }
            } else if (result.status == OTPStatus.INVALID_EMAIL) {
                _state.update { it.copy(errorMessage = "Please enter a valid email address.") }
            } else {
                _state.update { it.copy(errorMessage = "Failed to send code. Check your connection.") }
            }
            result
        }
    }

    fun setBudget(input: String): Boolean {
        val result = BudgetService.validateBudget(input, _state.value.currentTotalCost)
        if (result.isValid) {
            _state.update {
                it.copy(
                    currentBudget = result.formattedBudget,
                    errorMessage = null,
                    toastMessage = "Budget successfully updated!"
                )
            }
            syncToCloud()
            return true
        } else {
            _state.update { it.copy(errorMessage = result.errorMessage) }
            return false
        }
    }

    fun addPantryItem(name: String, qty: String, expDate: String) {
        var displayName = name
        if (name.trim() == "") {
            displayName = "New Food"
        }
        val newItem = PantryItem(name = displayName, qty = qty, expDate = expDate)
        PantryBackend.savedPantryItems.add(newItem)
        _state.update { it.copy(pantryItems = PantryBackend.savedPantryItems.toList()) }
        syncToCloud()
    }

    fun refreshPantry() {
        _state.update { it.copy(pantryItems = PantryBackend.savedPantryItems.toList()) }
    }

    fun sendChatMessage(message: String) {
        _state.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch(Dispatchers.IO) {
            val result = AIChatBot.askChefAI(message, _state.value.currentBudget)
            _state.update { it.copy(isLoading = false) }
        }
    }

    fun generateFromPantry(): String? {
        val prompt = Chatbackend.generatePromptFromPantry(PantryBackend.savedPantryItems)
        if (prompt == null) {
            if (PantryBackend.savedPantryItems.isEmpty()) {
                _state.update { it.copy(errorMessage = "Your pantry is empty. Add items first!") }
            } else {
                _state.update { it.copy(errorMessage = "All items in your pantry have expired.") }
            }
        } else {
            _state.update { it.copy(pendingPantryPrompt = prompt, isFromPantry = true) }
        }
        return prompt
    }

    fun saveRecipeToMenu(aiResponse: ParsedResponse) {
        val analysis = Chatbackend.analyzeRecipe(aiResponse, _state.value.currentBudget)
        Chatbackend.saveRecipeToMenu(aiResponse) { recipeName, ingredients, fullIngredients, checked, calories, protein, totalCost ->
            val missingLabel = ShoppingListBackend.computeMissingCount(ingredients, checked)
            _state.update {
                it.copy(
                    currentRecipeName = recipeName,
                    currentIngredients = ingredients,
                    fullRecipeIngredients = fullIngredients,
                    checkedIngredients = checked,
                    currentCalories = calories,
                    currentProtein = protein,
                    currentTotalCost = totalCost,
                    savedMissingIngredients = missingLabel
                )
            }
            syncToCloud()
        }
    }

    fun toggleIngredientCheck(index: Int, isChecked: Boolean) {
        val newChecked = _state.value.checkedIngredients.toMutableList()
        ShoppingListBackend.updateCheckedState(newChecked, index, isChecked)
        val missingLabel = ShoppingListBackend.computeMissingCount(_state.value.currentIngredients, newChecked)
        _state.update {
            it.copy(checkedIngredients = newChecked, savedMissingIngredients = missingLabel)
        }
    }

    fun completeShoppingList() {
        val updatedPantry = Chatbackend.deductIngredientsAndClearState(
            _state.value.fullRecipeIngredients,
            _state.value.checkedIngredients,
            PantryBackend.savedPantryItems
        )
        PantryBackend.savedPantryItems.clear()
        PantryBackend.savedPantryItems.addAll(updatedPantry)
        _state.update {
            it.copy(
                currentRecipeName = "No meal selected",
                currentIngredients = emptyList(),
                fullRecipeIngredients = emptyList(),
                checkedIngredients = emptyList(),
                currentCalories = "N/A",
                currentProtein = "N/A",
                currentTotalCost = 0.0,
                savedMissingIngredients = "",
                isFromPantry = false,
                pendingPantryPrompt = null,
                toastMessage = "Shopping list completed! Ingredients deducted from pantry.",
                pantryItems = PantryBackend.savedPantryItems.toList()
            )
        }
        syncToCloud()
    }

    fun clearRecipe() {
        _state.update {
            it.copy(
                currentRecipeName = "No meal selected",
                currentIngredients = emptyList(),
                fullRecipeIngredients = emptyList(),
                checkedIngredients = emptyList(),
                currentCalories = "N/A",
                currentProtein = "N/A",
                currentTotalCost = 0.0,
                savedMissingIngredients = "",
                isFromPantry = false,
                pendingPantryPrompt = null
            )
        }
        syncToCloud()
    }
}
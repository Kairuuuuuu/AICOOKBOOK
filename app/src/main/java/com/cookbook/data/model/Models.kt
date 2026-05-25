package com.cookbook.data.model

data class PantryItem(
    val name: String = "New Food",
    val qty: String = "",
    val expDate: String = ""
)

data class ParsedResponse(
    val recipeName: String = "",
    val ingredients: List<String> = emptyList(),
    val instructions: List<String> = emptyList(),
    val hasRecipe: Boolean = false,
    val totalEstimatedCost: Double = 0.0,
    val calories: String = "N/A",
    val protein: String = "N/A"
)

data class RecipeAnalysisResult(
    val status: BudgetStatus,
    val finalOutOfPocketCost: Double,
    val currentBudget: Double
)

enum class BudgetStatus {
    OK, NO_BUDGET, INSUFFICIENT_FUNDS
}

data class OTPResult(
    val status: OTPStatus,
    val sentCode: String = ""
)

enum class OTPStatus {
    SUCCESS, INVALID_EMAIL, CONNECTION_ERROR
}

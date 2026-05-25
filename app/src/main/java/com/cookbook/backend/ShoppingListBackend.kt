package com.cookbook.backend


object ShoppingListBackend {

    fun computeMissingCount(ingredients: List<String>, checkedState: List<Boolean>): String {
        if (ingredients.isEmpty()) return "Missing: None"
        val missing = checkedState.count { !it }
        return if (missing == 0) "Missing: None" else "Missing: $missing items"
    }

    fun updateCheckedState(checkedState: MutableList<Boolean>, index: Int, isChecked: Boolean) {
        if (index in checkedState.indices) {
            checkedState[index] = isChecked
        }
    }
}

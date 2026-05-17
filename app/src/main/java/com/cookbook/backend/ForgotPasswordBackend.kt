package com.cookbook.backend

object ForgotPasswordBackend {

    fun processPasswordChange(
        email: String,
        newPassword: String,
        confirmPassword: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (newPassword.isBlank() || confirmPassword.isBlank()) {
            onError("Please enter a new password!")
            return
        }
        if (newPassword != confirmPassword) {
            onError("Passwords do not match!")
            return
        }
        if (newPassword.length < 6) {
            onError("Password must be at least 6 characters!")
            return
        }

        FirebaseManager.changePassword(newPassword) { result ->
            if (result == "SUCCESS") {
                onSuccess()
            } else {
                onError(result)
            }
        }
    }
}

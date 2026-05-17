package com.cookbook.backend

object ChangePasswordService {

    fun updatePassword(
        email: String,
        currentPassword: String,
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

        FirebaseManager.loginUser(email, currentPassword) { loginResult ->
            if (loginResult != "SUCCESS") {
                onError("Current password is incorrect.")
            } else {
                FirebaseManager.changePassword(newPassword) { result ->
                    if (result == "SUCCESS") {
                        onSuccess()
                    } else {
                        onError(result)
                    }
                }
            }
        }
    }
}

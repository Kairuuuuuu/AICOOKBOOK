package com.cookbook.backend

object SignUpBackend {

    fun attemptSignUp(
        email: String,
        password: String,
        confirmPassword: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (password.isBlank() || confirmPassword.isBlank()) {
            onError("Password fields cannot be empty!")
            return
        }
        if (password != confirmPassword) {
            onError("Passwords do not match!")
            return
        }
        if (password.length < 6) {
            onError("Password must be at least 6 characters!")
            return
        }

        FirebaseManager.signUpUser(email, password) { result ->
            if (result == "SUCCESS") {
                onSuccess()
            } else {
                onError(result)
            }
        }
    }
}

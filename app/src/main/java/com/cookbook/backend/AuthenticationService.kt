package com.cookbook.backend

object AuthenticationService {

    fun attemptLogin(email: String, password: String, onResult: (String, String) -> Unit, onError: (String) -> Unit) {
        if (email.trim() == "" || password.trim() == "") {
            onError("Please fill in all fields!")
            return
        }
        if (password.length < 6) {
            onError("Password must be at least 6 characters!")
            return
        }

        FirebaseManager.loginUser(email, password) { result ->
            if (result == "SUCCESS") {
                val localPart = email.substringBefore("@")
                var firstName = "User"
                if (localPart.isNotEmpty()) {
                    firstName = localPart.replaceFirstChar { it.uppercase() }
                }
                onResult(email, firstName)
            } else {
                onError(result)
            }
        }
    }
}
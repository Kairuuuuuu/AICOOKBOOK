package com.cookbook.backend

import com.cookbook.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FirebaseFirestore
import com.cookbook.data.model.PantryItem
import java.util.HashMap
import java.util.ArrayList

object FirebaseManager {

    fun connect(context: android.content.Context) {
        if (FirebaseApp.getApps(context).isEmpty()) {
            val options = FirebaseOptions.Builder()
                .setApiKey(BuildConfig.FIREBASE_API_KEY)
                .setApplicationId("1:863089731969:android:1057d7248353846c4ee080")
                .setProjectId("ai-cookbook-f347b")
                .build()
            FirebaseApp.initializeApp(context, options)
        }
    }

    fun signUpUser(email: String, password: String, onResult: (String) -> Unit) {
        val firebaseAuth = FirebaseAuth.getInstance()
        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onResult("SUCCESS")
                } else {
                    val msg = mapFirebaseError(task.exception)
                    onResult(msg)
                }
            }
    }

    fun loginUser(email: String, password: String, onResult: (String) -> Unit) {
        val firebaseAuth = FirebaseAuth.getInstance()
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onResult("SUCCESS")
                } else {
                    val msg = mapFirebaseError(task.exception)
                    onResult(msg)
                }
            }
    }

    fun changePassword(newPassword: String, onResult: (String) -> Unit) {
        val firebaseAuth = FirebaseAuth.getInstance()
        val user = firebaseAuth.currentUser
        if (user != null) {
            user.updatePassword(newPassword)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        onResult("SUCCESS")
                    } else {
                        val msg = mapFirebaseError(task.exception)
                        onResult(msg)
                    }
                }
        } else {
            onResult("No authenticated user found. Please log in again.")
        }
    }

    fun sendPasswordResetEmail(email: String, onResult: (Boolean, String) -> Unit) {
        val firebaseAuth = FirebaseAuth.getInstance()
        firebaseAuth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onResult(true, "Password reset email sent.")
                } else {
                    val msg = mapFirebaseError(task.exception)
                    onResult(false, msg)
                }
            }
    }

    fun logout() {
        val firebaseAuth = FirebaseAuth.getInstance()
        firebaseAuth.signOut()
    }

    fun saveUserDataToCloud(budget: String, pantryList: List<PantryItem>, currentRecipe: String) {
        val firebaseAuth = FirebaseAuth.getInstance()
        val currentUser = firebaseAuth.currentUser
        if (currentUser == null) {
            return
        }
        val userId = currentUser.uid
        val db = FirebaseFirestore.getInstance()

        val mappedPantry = ArrayList<HashMap<String, String>>()
        for (item in pantryList) {
            val ingredientMap = HashMap<String, String>()
            ingredientMap.put("name", item.name)
            ingredientMap.put("qty", item.qty)
            ingredientMap.put("expDate", item.expDate)
            mappedPantry.add(ingredientMap)
        }

        val userBundle = HashMap<String, Any>()
        userBundle.put("budget", budget)
        userBundle.put("pantry", mappedPantry)
        userBundle.put("currentRecipe", currentRecipe)

        db.collection("users").document(userId).set(userBundle)
    }

    fun loadUserDataFromCloud(onComplete: (budget: String, pantry: List<PantryItem>, recipe: String) -> Unit) {
        val firebaseAuth = FirebaseAuth.getInstance()
        val currentUser = firebaseAuth.currentUser
        if (currentUser == null) {
            return
        }
        val userId = currentUser.uid
        val db = FirebaseFirestore.getInstance()
        
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    var savedBudget = document.getString("budget")
                    if (savedBudget == null) {
                        savedBudget = "Php 0.00"
                    }

                    var savedRecipe = document.getString("currentRecipe")
                    if (savedRecipe == null) {
                        savedRecipe = "No meal selected"
                    }

                    val savedPantry = ArrayList<PantryItem>()
                    val rawCloudData = document.get("pantry")
                    if (rawCloudData != null) {
                        val cloudList = rawCloudData as List<Map<String, String>>
                        for (rawMap in cloudList) {
                            val nameStr = rawMap.get("name").toString()
                            val qtyStr = rawMap.get("qty").toString()
                            val expDateStr = rawMap.get("expDate").toString()

                            val cleanItem = PantryItem(
                                name = nameStr,
                                qty = qtyStr,
                                expDate = expDateStr
                            )
                            savedPantry.add(cleanItem)
                        }
                    }
                    onComplete(savedBudget, savedPantry, savedRecipe)
                } else {
                    val freshPantry = ArrayList<PantryItem>()
                    onComplete("Php 0.00", freshPantry, "No meal selected")
                }
            }
    }

    private fun mapFirebaseError(exception: Exception?): String {
        if (exception !is FirebaseAuthException) {
            if (exception != null) {
                return exception.message.toString()
            }
            return "An unknown error occurred."
        }
        return when (exception.errorCode) {
            "ERROR_INVALID_EMAIL" -> "Invalid email address."
            "ERROR_WRONG_PASSWORD" -> "Incorrect password."
            "ERROR_USER_NOT_FOUND" -> "No account found with this email."
            "ERROR_EMAIL_ALREADY_IN_USE" -> "This email is already registered."
            "ERROR_WEAK_PASSWORD" -> "Password must be at least 6 characters."
            "ERROR_NETWORK_REQUEST_FAILED" -> "Network error. Please check your connection."
            "ERROR_TOO_MANY_REQUESTS" -> "Too many attempts. Please try again later."
            "ERROR_USER_DISABLED" -> "This account has been disabled."
            "ERROR_OPERATION_NOT_ALLOWED" -> "This operation is not allowed."
            "ERROR_REQUIRES_RECENT_LOGIN" -> "Please log out and log in again before changing your password."
            else -> exception.message.toString()
        }
    }
}
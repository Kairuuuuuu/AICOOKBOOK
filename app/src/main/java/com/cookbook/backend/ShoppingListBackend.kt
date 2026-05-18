package com.cookbook.backend

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.net.HttpURLConnection
import java.net.URL

object ShoppingListBackend {

    private val imageCache = mutableMapOf<String, Bitmap>()

    fun fetchRecipeImage(searchKeyword: String, onResult: (Bitmap?) -> Unit) {
        if (searchKeyword == "No meal selected" || searchKeyword == "AI Suggested Recipe") {
            onResult(null)
            return
        }

        val cachedImage = imageCache[searchKeyword]
        if (cachedImage != null) {
            onResult(cachedImage)
            return
        }

        Thread {
            try {
                val safeName = searchKeyword.replace(" ", "%20")
                val prompt = "Authentic%20${safeName}%20dish,%20realistic%20food%20photography,%20restaurant%20plating"
                val urlStr = "https://image.pollinations.ai/prompt/$prompt?width=320&height=240&nologo=true&model=flux"

                val connection = URL(urlStr).openConnection() as HttpURLConnection
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                val bitmap = BitmapFactory.decodeStream(connection.inputStream)
                if (bitmap != null) {
                    imageCache[searchKeyword] = bitmap
                }
                onResult(bitmap)
            } catch (_: Exception) {
                onResult(null)
            }
        }.start()
    }

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

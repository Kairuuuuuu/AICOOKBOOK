package com.cookbook.backend

import com.cookbook.BuildConfig
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.cookbook.data.model.ParsedResponse
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

object AIChatBot {

    private val httpClient = OkHttpClient()

    fun askChefAI(userMessage: String, budget: String = ""): ParsedResponse {
        val apiKey = BuildConfig.GROQ_API_KEY
        if (apiKey.isBlank()) return ParsedResponse()

        return try {
            val prompt = buildPrompt(userMessage, budget)
            val requestBody = JsonObject().apply {
                addProperty("model", "llama-3.1-8b-instant")
                add("messages", com.google.gson.JsonArray().apply {
                    add(JsonObject().apply {
                        addProperty("role", "user")
                        addProperty("content", prompt)
                    })
                })
                add("response_format", JsonObject().apply {
                    addProperty("type", "json_object")
                })
                addProperty("temperature", 0.7)
                addProperty("max_tokens", 1024)
            }

            val request = Request.Builder()
                .url("https://api.groq.com/openai/v1/chat/completions")
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer $apiKey")
                .header("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:102.0) Gecko/102.0 Firefox/102.0")
                .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                parseGroqResponse(response.body?.string() ?: "")
            } else {
                val errorBody = response.body?.string() ?: "No error body"
                ParsedResponse(recipeName = "API Error ${response.code}: $errorBody", hasRecipe = false)
            }
        } catch (e: Exception) {
            ParsedResponse(recipeName = "Network/Exception Error: ${e.message}", hasRecipe = false)
        }
    }

    private fun buildPrompt(userMessage: String, budget: String): String {
        val basePrompt = """You are Chef Dirk, a friendly Filipino master chef and nutritionist. 
            |You help users generate delicious, authentic Filipino and international recipes.
            |Additional Rule: when the user inputs something inappropriate just reply with this
            |message: I'm built to be your digital sous chef, so I can only help out with food, 
            |recipes, and cooking techniques! Let’s get back to the kitchen—what ingredients are 
            |we working with today?
            |You must ALWAYS respond in this EXACT JSON format with NO additional text:
            |{
            |  "recipe_name": "Name of the dish",
            |  "ingredients": [
            |    {"name": "ingredient with quantity", "estimated_price_php": 0.00},
            |    ...
            |  ],
            |  "nutrition": {
            |    "calories": "XXX kcal per serving",
            |    "protein": "XXg protein per serving"
            |  },
            |  "total_estimated_cost_php": 0.00
            |}
            |Make sure to: use realistic Philippine market prices in PHP, list all ingredients with quantities, 
            |estimate cost per ingredient and provide a total.
        """.trimMargin()

        if (budget.isNotBlank() && budget != "Php 0") {
            return "$basePrompt\n\nThe user's budget is $budget. Make sure the total cost stays within this budget.\n\nUser request: $userMessage"
        }
        return "$basePrompt\n\nUser request: $userMessage"
    }

    private fun parseGroqResponse(responseBody: String): ParsedResponse {
        return try {
            val root = JsonParser.parseString(responseBody).asJsonObject
            val choices = root.getAsJsonArray("choices")
            val message = choices[0].asJsonObject.getAsJsonObject("message")
            val content = message.get("content").asString
            
            val start = content.indexOf('{')
            val end = content.lastIndexOf('}')
            if (start == -1 || end == -1 || end < start) {
                return ParsedResponse()
            }
            
            val cleaned = content.substring(start, end + 1)
            val recipeJson = JsonParser.parseString(cleaned).asJsonObject

            val ingredients = mutableListOf<String>()
            val ingredientsArray = recipeJson.getAsJsonArray("ingredients")
            ingredientsArray?.forEach { item ->
                val obj = item.asJsonObject
                val name = obj.get("name")?.asString ?: ""
                val price = obj.get("estimated_price_php")?.asDouble ?: 0.0
                ingredients.add("$name (Php %.2f)".format(price))
            }

            val nutrition = recipeJson.getAsJsonObject("nutrition")
            val calories = nutrition?.get("calories")?.asString ?: "N/A"
            val protein = nutrition?.get("protein")?.asString ?: "N/A"

            ParsedResponse(
                recipeName = recipeJson.get("recipe_name")?.asString ?: "AI Suggested Recipe",
                ingredients = ingredients,
                hasRecipe = ingredients.isNotEmpty(),
                totalEstimatedCost = recipeJson.get("total_estimated_cost_php")?.asDouble ?: 0.0,
                calories = calories,
                protein = protein
            )
        } catch (e: Exception) {
            ParsedResponse(recipeName = "Parsing Error: ${e.message}", hasRecipe = false)
        }
    }
}

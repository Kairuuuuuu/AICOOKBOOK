package com.cookbook.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cookbook.ui.theme.*
import com.cookbook.ui.viewmodel.CookbookViewModel

@Composable
fun AddBudgetDialog(
    viewModel: CookbookViewModel,
    onDismiss: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    var budgetInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Set Your Budget",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = DarkGray,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Set a spending limit for your kitchen",
                    style = MaterialTheme.typography.bodySmall,
                    color = MediumGray
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = budgetInput,
                    onValueChange = { budgetInput = it; viewModel.clearError() },
                    label = { Text("Your Budget") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.MonetizationOn,
                            contentDescription = null,
                            tint = GreenPrimary
                        )
                    },
                    placeholder = {
                        Text(
                            text = "e.g. 500",
                            color = LightGray
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                if (state.errorMessage != null) {
                    Text(
                        text = state.errorMessage!!,
                        color = ErrorRed,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val success = viewModel.setBudget(budgetInput)
                    if (success) {
                        onDismiss()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(25.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
            ) {
                Text("Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MediumGray)
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

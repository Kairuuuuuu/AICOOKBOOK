package com.cookbook.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.cookbook.ui.theme.*
import com.cookbook.ui.viewmodel.CookbookViewModel

@Composable
fun ShoppingListSheet(
    viewModel: CookbookViewModel,
    onDismiss: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val allChecked = state.checkedIngredients.isNotEmpty() &&
            state.checkedIngredients.all { it }


    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.8f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFEBEBEB))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Shopping List",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = DarkGray
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = MediumGray)
                    }
                }

                // Recipe name
                Text(
                    text = state.currentRecipeName,
                    style = MaterialTheme.typography.titleSmall,
                    color = GreenPrimary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )


                // Missing label
                Text(
                    text = state.savedMissingIngredients,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (state.savedMissingIngredients.contains("None")) SuccessGreen else WarningOrange,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )

                // Ingredient list
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                ) {
                    itemsIndexed(state.currentIngredients) { index, ingredient ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.toggleIngredientCheck(
                                        index,
                                        !state.checkedIngredients.getOrElse(index) { false }
                                    )
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = state.checkedIngredients.getOrElse(index) { false },
                                onCheckedChange = { viewModel.toggleIngredientCheck(index, it) },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = GreenPrimary,
                                    uncheckedColor = LightGray
                                )
                            )
                            Text(
                                text = ingredient,
                                style = MaterialTheme.typography.bodyMedium,
                                color = DarkGray,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Total cost
                Text(
                    text = "Total Cost: Php %.2f".format(state.currentTotalCost),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = DarkGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )

                // Done / Close
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.completeShoppingList()
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(22.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                        enabled = allChecked
                    ) {
                        Text("Done", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(22.dp)
                    ) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

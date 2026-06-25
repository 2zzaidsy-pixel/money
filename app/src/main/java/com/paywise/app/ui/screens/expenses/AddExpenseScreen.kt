package com.paywise.app.ui.screens.expenses

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paywise.app.data.local.PreferencesManager
import com.paywise.app.data.repository.PayWiseRepository
import com.paywise.app.domain.model.*
import com.paywise.app.firebase.FirebaseService
import com.paywise.app.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class AddExpenseViewModel @Inject constructor(
    private val repository: PayWiseRepository,
    private val preferencesManager: PreferencesManager,
    private val firebaseService: FirebaseService
) : ViewModel() {

    var amount by mutableStateOf("")
    var selectedCategory by mutableStateOf(ExpenseCategory.RESTAURANTS)
    var note by mutableStateOf("")
    var date by mutableStateOf(System.currentTimeMillis())
    var isLoading by mutableStateOf(false)
    var showCategoryPicker by mutableStateOf(false)
    var isEditing by mutableStateOf(false)
    var editingExpenseId by mutableStateOf<String?>(null)

    fun loadExpense(expenseId: String) {
        viewModelScope.launch {
            val userId = preferencesManager.userId.first() ?: return@launch
            val expense = repository.getExpenseById(userId, expenseId)
            if (expense != null) {
                amount = expense.amount.toString()
                selectedCategory = expense.category
                note = expense.note
                date = expense.date
                isEditing = true
                editingExpenseId = expense.id
            }
        }
    }

    fun save(onSaved: () -> Unit) {
        viewModelScope.launch {
            isLoading = true
            try {
                val userId = preferencesManager.userId.first() ?: return@launch
                val expenseAmount = amount.toDoubleOrNull() ?: 0.0
                if (expenseAmount <= 0) return@launch

                val expense = Expense(
                    id = editingExpenseId ?: UUID.randomUUID().toString(),
                    userId = userId,
                    amount = expenseAmount,
                    category = selectedCategory,
                    date = date,
                    note = note
                )

                if (isEditing && editingExpenseId != null) {
                    repository.updateExpense(expense)
                } else {
                    repository.addExpense(expense)
                }
                firebaseService.syncExpense(expense)
                onSaved()
            } finally {
                isLoading = false
            }
        }
    }

    fun getCategorySections(): List<Pair<String, List<ExpenseCategory>>> {
        return listOf(
            "Essential" to listOf(
                ExpenseCategory.RENT, ExpenseCategory.ELECTRICITY, ExpenseCategory.WATER,
                ExpenseCategory.INTERNET, ExpenseCategory.LOANS, ExpenseCategory.FUEL,
                ExpenseCategory.GROCERIES, ExpenseCategory.MEDICAL,
                ExpenseCategory.HOME_MAINTENANCE, ExpenseCategory.FAMILY_EMERGENCY
            ),
            "Secondary" to listOf(
                ExpenseCategory.RESTAURANTS, ExpenseCategory.COFFEE, ExpenseCategory.ENTERTAINMENT,
                ExpenseCategory.SHOPPING, ExpenseCategory.SUBSCRIPTIONS, ExpenseCategory.CAR_REPAIRS
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(
    expenseId: String?,
    onSaved: () -> Unit,
    onBack: () -> Unit,
    viewModel: AddExpenseViewModel = hiltViewModel()
) {
    LaunchedEffect(expenseId) {
        if (expenseId != null) {
            viewModel.loadExpense(expenseId)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    if (expenseId != null) "Edit Expense" else "Add Expense",
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            // Amount
            OutlinedTextField(
                value = viewModel.amount,
                onValueChange = { viewModel.amount = it },
                label = { Text("Amount") },
                leadingIcon = { Text("$", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Category picker
            Text(
                text = "Category",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Surface(
                onClick = { viewModel.showCategoryPicker = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(36.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = getCategoryColor(viewModel.selectedCategory).copy(alpha = 0.1f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = viewModel.selectedCategory.displayName.take(2),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = getCategoryColor(viewModel.selectedCategory)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = viewModel.selectedCategory.displayName,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
            }

            // Category picker dialog
            if (viewModel.showCategoryPicker) {
                AlertDialog(
                    onDismissRequest = { viewModel.showCategoryPicker = false },
                    title = { Text("Select Category") },
                    text = {
                        Column {
                            viewModel.getCategorySections().forEach { (sectionName, categories) ->
                                Text(
                                    text = sectionName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                                categories.chunked(2).forEach { row ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        row.forEach { cat ->
                                            FilterChip(
                                                selected = viewModel.selectedCategory == cat,
                                                onClick = {
                                                    viewModel.selectedCategory = cat
                                                    viewModel.showCategoryPicker = false
                                                },
                                                label = {
                                                    Text(
                                                        cat.displayName,
                                                        style = MaterialTheme.typography.bodySmall
                                                    )
                                                },
                                                modifier = Modifier.weight(1f),
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = getCategoryColor(cat).copy(alpha = 0.2f),
                                                    selectedLabelColor = getCategoryColor(cat)
                                                )
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { viewModel.showCategoryPicker = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Note
            OutlinedTextField(
                value = viewModel.note,
                onValueChange = { viewModel.note = it },
                label = { Text("Note (optional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Save button
            Button(
                onClick = { viewModel.save(onSaved) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = !viewModel.isLoading && viewModel.amount.toDoubleOrNull() != null && (viewModel.amount.toDoubleOrNull() ?: 0.0) > 0,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
            ) {
                if (viewModel.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = if (expenseId != null) "Update Expense" else "Save Expense",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

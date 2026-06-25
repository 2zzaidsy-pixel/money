package com.paywise.app.ui.screens.expenses

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.paywise.app.data.local.PreferencesManager
import com.paywise.app.data.repository.PayWiseRepository
import com.paywise.app.domain.model.*
import com.paywise.app.firebase.FirebaseService
import com.paywise.app.ui.components.ModernCard
import com.paywise.app.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class ExpensesViewModel @Inject constructor(
    private val repository: PayWiseRepository,
    private val preferencesManager: PreferencesManager,
    private val firebaseService: FirebaseService
) : ViewModel() {

    private val userId = MutableStateFlow("")
    val expenses = userId.flatMapLatest { uid ->
        if (uid.isBlank()) flowOf(emptyList()) else repository.getExpenses(uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    var searchQuery by mutableStateOf("")
    var selectedCategory by mutableStateOf<ExpenseCategory?>(null)
    var showDeleteDialog by mutableStateOf(false)
    var expenseToDelete by mutableStateOf<Expense?>(null)

    init {
        viewModelScope.launch {
            preferencesManager.userId.collectLatest { uid ->
                if (!uid.isNullOrBlank()) userId.value = uid
            }
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
            firebaseService.deleteExpenseFromCloud(expense.userId, expense.id)
            showDeleteDialog = false
            expenseToDelete = null
        }
    }

    fun confirmDelete(expense: Expense) {
        expenseToDelete = expense
        showDeleteDialog = true
    }

    fun getFilteredExpenses(): List<Expense> {
        return expenses.value.filter { expense ->
            val matchesSearch = searchQuery.isBlank() ||
                    expense.note.contains(searchQuery, ignoreCase = true) ||
                    expense.category.displayName.contains(searchQuery, ignoreCase = true)
            val matchesCategory = selectedCategory == null || expense.category == selectedCategory
            matchesSearch && matchesCategory
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen(
    onAddExpense: () -> Unit,
    onEditExpense: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: ExpensesViewModel = hiltViewModel()
) {
    val expenses by viewModel.expenses.collectAsStateWithLifecycle()
    val filteredExpenses = viewModel.getFilteredExpenses()

    Column(modifier = Modifier.fillMaxSize()) {
        // Top bar
        TopAppBar(
            title = {
                Text("Expenses", fontWeight = FontWeight.Bold)
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                IconButton(onClick = onAddExpense) {
                    Icon(Icons.Default.Add, contentDescription = "Add Expense")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        )

        // Search bar
        OutlinedTextField(
            value = viewModel.searchQuery,
            onValueChange = { viewModel.searchQuery = it },
            placeholder = { Text("Search expenses...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        // Category filter chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = viewModel.selectedCategory == null,
                onClick = { viewModel.selectedCategory = null },
                label = { Text("All") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = PrimaryGreen.copy(alpha = 0.2f),
                    selectedLabelColor = PrimaryGreen
                )
            )
            ExpenseCategory.entries.forEach { category ->
                FilterChip(
                    selected = viewModel.selectedCategory == category,
                    onClick = {
                        viewModel.selectedCategory = if (viewModel.selectedCategory == category) null else category
                    },
                    label = { Text(category.displayName) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = getCategoryColor(category).copy(alpha = 0.2f),
                        selectedLabelColor = getCategoryColor(category)
                    )
                )
            }
        }

        // Expense list
        if (expenses.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.ReceiptLong,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No expenses yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextSecondary
                    )
                    Text(
                        text = "Tap + to add your first expense",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredExpenses, key = { it.id }) { expense ->
                    ExpenseCard(
                        expense = expense,
                        onEdit = { onEditExpense(expense.id) },
                        onDelete = { viewModel.confirmDelete(expense) }
                    )
                }
            }
        }
    }

    // Delete dialog
    if (viewModel.showDeleteDialog && viewModel.expenseToDelete != null) {
        AlertDialog(
            onDismissRequest = { viewModel.showDeleteDialog = false },
            title = { Text("Delete Expense") },
            text = { Text("Are you sure you want to delete this expense?") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteExpense(viewModel.expenseToDelete!!) }) {
                    Text("Delete", color = AccentRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ExpenseCard(
    expense: Expense,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val categoryColor = getCategoryColor(expense.category)
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    ModernCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                color = categoryColor.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = expense.category.displayName.take(2),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = categoryColor
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.category.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                if (expense.note.isNotBlank()) {
                    Text(
                        text = expense.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                Text(
                    text = dateFormat.format(Date(expense.date)),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }

            Text(
                text = "-${String.format("%.0f", expense.amount)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = AccentRed
            )

            IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp), tint = TextSecondary)
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(18.dp), tint = AccentRed.copy(alpha = 0.7f))
            }
        }
    }
}

fun getCategoryColor(category: ExpenseCategory): Color = when (category) {
    ExpenseCategory.RENT, ExpenseCategory.ELECTRICITY, ExpenseCategory.WATER -> AccentBlue
    ExpenseCategory.INTERNET, ExpenseCategory.SUBSCRIPTIONS -> AccentPurple
    ExpenseCategory.LOANS -> AccentOrange
    ExpenseCategory.FUEL -> Color(0xFF90A4AE)
    ExpenseCategory.GROCERIES -> PrimaryGreen
    ExpenseCategory.RESTAURANTS, ExpenseCategory.COFFEE -> AccentOrange
    ExpenseCategory.ENTERTAINMENT -> AccentPurple
    ExpenseCategory.SHOPPING -> Color(0xFFFF4081)
    ExpenseCategory.MEDICAL -> AccentRed
    ExpenseCategory.CAR_REPAIRS -> Color(0xFF795548)
    ExpenseCategory.HOME_MAINTENANCE -> Color(0xFF607D8B)
    ExpenseCategory.FAMILY_EMERGENCY -> AccentRed
}

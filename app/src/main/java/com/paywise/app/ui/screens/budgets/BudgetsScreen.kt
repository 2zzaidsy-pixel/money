package com.paywise.app.ui.screens.budgets

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import com.paywise.app.ui.screens.expenses.getCategoryColor
import com.paywise.app.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class BudgetsViewModel @Inject constructor(
    private val repository: PayWiseRepository,
    private val preferencesManager: PreferencesManager,
    private val firebaseService: FirebaseService
) : ViewModel() {

    private val userId = MutableStateFlow("")
    private val calendar = Calendar.getInstance()
    val currentMonth = calendar.get(Calendar.MONTH)
    val currentYear = calendar.get(Calendar.YEAR)

    val budgets = userId.flatMapLatest { uid ->
        if (uid.isBlank()) flowOf(emptyList()) else repository.getBudgets(uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    var budgetWithSpending by mutableStateOf<List<BudgetWithSpending>>(emptyList())
    var showAddDialog by mutableStateOf(false)
    var newCategory by mutableStateOf(ExpenseCategory.RESTAURANTS)
    var newLimit by mutableStateOf("")
    var editBudgetId by mutableStateOf<String?>(null)

    init {
        viewModelScope.launch {
            preferencesManager.userId.collectLatest { uid ->
                if (!uid.isNullOrBlank()) userId.value = uid
            }
        }
        loadBudgetSpending()
    }

    fun loadBudgetSpending() {
        viewModelScope.launch {
            val uid = userId.first()
            if (uid.isBlank()) return@launch
            val allBudgets = budgets.value
            val items = allBudgets.map { budget ->
                repository.getBudgetWithSpending(uid, budget)
            }
            budgetWithSpending = items
        }
    }

    fun saveBudget() {
        viewModelScope.launch {
            val uid = userId.first() ?: return@launch
            val limit = newLimit.toDoubleOrNull() ?: return@launch
            val budget = Budget(
                id = editBudgetId ?: UUID.randomUUID().toString(),
                userId = uid,
                category = newCategory,
                limitAmount = limit,
                month = currentMonth,
                year = currentYear
            )
            repository.addBudget(budget)
            firebaseService.syncBudget(budget)
            showAddDialog = false
            newLimit = ""
            editBudgetId = null
            loadBudgetSpending()
        }
    }

    fun deleteBudget(budgetId: String) {
        viewModelScope.launch {
            val uid = userId.first() ?: return@launch
            val budget = repository.getBudgetById(uid, budgetId) ?: return@launch
            repository.deleteBudget(budget)
            loadBudgetSpending()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetsScreen(
    onBack: () -> Unit,
    viewModel: BudgetsViewModel = hiltViewModel()
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Budgets", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                IconButton(onClick = { viewModel.showAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Budget")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        )

        if (viewModel.budgetWithSpending.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.AccountTree,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No budgets set", style = MaterialTheme.typography.titleMedium, color = TextSecondary)
                    Text("Tap + to create a budget", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Summary card
                val totalBudget = viewModel.budgetWithSpending.sumOf { it.budget.limitAmount }
                val totalSpent = viewModel.budgetWithSpending.sumOf { it.spentAmount }
                ModernCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Budget Overview", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = String.format("%.0f", totalBudget),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryGreen
                                )
                                Text("Budgeted", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = String.format("%.0f", totalSpent),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentOrange
                                )
                                Text("Spent", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = String.format("%.0f", (totalBudget - totalSpent).coerceAtLeast(0.0)),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (totalBudget > totalSpent) AccentBlue else AccentRed
                                )
                                Text("Remaining", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                        }
                    }
                }

                viewModel.budgetWithSpending.forEach { bws ->
                    BudgetCard(
                        budgetWithSpending = bws,
                        onDelete = { viewModel.deleteBudget(bws.budget.id) }
                    )
                }
            }
        }
    }

    // Add budget dialog
    if (viewModel.showAddDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.showAddDialog = false },
            title = { Text("Create Budget") },
            text = {
                Column {
                    Text("Category", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    ExpenseCategory.entries.filter { !it.isEssential }.forEach { cat ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.newCategory = cat }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = viewModel.newCategory == cat,
                                onClick = { viewModel.newCategory = cat }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(cat.displayName)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = viewModel.newLimit,
                        onValueChange = { viewModel.newLimit = it },
                        label = { Text("Monthly Limit") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.saveBudget() },
                    enabled = viewModel.newLimit.toDoubleOrNull() != null
                ) {
                    Text("Save", color = PrimaryGreen)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun BudgetCard(
    budgetWithSpending: BudgetWithSpending,
    onDelete: () -> Unit
) {
    val bws = budgetWithSpending
    val color = when {
        bws.isExceeded -> AccentRed
        bws.percentageUsed > 0.8f -> AccentOrange
        else -> PrimaryGreen
    }

    ModernCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(36.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = getCategoryColor(bws.budget.category).copy(alpha = 0.1f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = bws.budget.category.displayName.take(2),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = getCategoryColor(bws.budget.category)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = bws.budget.category.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AccentRed.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color(0xFF2D333B))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction = bws.percentageUsed.coerceIn(0f, 1f))
                        .clip(RoundedCornerShape(5.dp))
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(color, color.copy(alpha = 0.7f))
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${String.format("%.0f", bws.spentAmount)} / ${String.format("%.0f", bws.budget.limitAmount)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Text(
                    text = "${(bws.percentageUsed * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }

            if (bws.isExceeded) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Budget exceeded by ${String.format("%.0f", bws.spentAmount - bws.budget.limitAmount)}!",
                    style = MaterialTheme.typography.bodySmall,
                    color = AccentRed,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

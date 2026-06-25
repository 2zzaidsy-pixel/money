package com.paywise.app.ui.screens.goals

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import com.paywise.app.data.repository.PayWiseRepository
import com.paywise.app.domain.model.FinancialGoal
import com.paywise.app.domain.model.GoalProgress
import com.paywise.app.ui.components.ModernCard
import com.paywise.app.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class GoalDetailViewModel @Inject constructor(
    private val repository: PayWiseRepository
) : ViewModel() {

    var goal by mutableStateOf<FinancialGoal?>(null)
    var progress by mutableStateOf<GoalProgress?>(null)
    var addAmount by mutableStateOf("")
    var isLoading by mutableStateOf(false)

    fun loadGoal(goalId: String) {
        viewModelScope.launch {
            isLoading = true
            val g = repository.getGoalById("", goalId)
            goal = g
            if (g != null) {
                progress = repository.calculateGoalProgress(g)
            }
            isLoading = false
        }
    }

    fun addToGoal(onSuccess: () -> Unit) {
        val amount = addAmount.toDoubleOrNull() ?: return
        if (amount <= 0) return
        viewModelScope.launch {
            goal?.let { g ->
                val newAmount = g.currentAmount + amount
                repository.updateGoalProgress(g.id, newAmount)
                goal = g.copy(currentAmount = newAmount)
                progress = repository.calculateGoalProgress(g.copy(currentAmount = newAmount))
                addAmount = ""
                onSuccess()
            }
        }
    }

    fun deleteGoal(onDeleted: () -> Unit) {
        viewModelScope.launch {
            goal?.let { g ->
                repository.deleteGoal(g)
                onDeleted()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalDetailScreen(
    goalId: String,
    onBack: () -> Unit,
    viewModel: GoalDetailViewModel = hiltViewModel()
) {
    LaunchedEffect(goalId) {
        viewModel.loadGoal(goalId)
    }

    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(viewModel.goal?.title ?: "Goal Details", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                if (viewModel.goal != null) {
                    var showDeleteConfirm by remember { mutableStateOf(false) }
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete goal", tint = AccentRed)
                    }
                    if (showDeleteConfirm) {
                        AlertDialog(
                            onDismissRequest = { showDeleteConfirm = false },
                            title = { Text("Delete Goal") },
                            text = { Text("Are you sure you want to delete this goal?") },
                            confirmButton = {
                                TextButton(onClick = {
                                    viewModel.deleteGoal(onDeleted = onBack)
                                    showDeleteConfirm = false
                                }) { Text("Delete", color = AccentRed) }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
                            }
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
        )

        if (viewModel.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryGreen)
            }
            return
        }

        val g = viewModel.goal
        val p = viewModel.progress
        if (g == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Goal not found", color = TextSecondary)
            }
            return
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            ModernCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${((p?.percentageComplete ?: 0f) * 100).toInt()}%",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryGreen
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { p?.percentageComplete ?: 0f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp)),
                        color = PrimaryGreen,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            ModernCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    DetailRow(label = "Target Amount", value = "%.2f".format(g.targetAmount))
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFF2D333B))
                    DetailRow(label = "Current Amount", value = "%.2f".format(g.currentAmount))
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFF2D333B))
                    DetailRow(label = "Remaining", value = "%.2f".format(p?.remainingAmount ?: (g.targetAmount - g.currentAmount)))
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFF2D333B))
                    DetailRow(label = "Deadline", value = dateFormat.format(Date(g.deadline)))
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFF2D333B))
                    DetailRow(
                        label = "Status",
                        value = if (p?.isOnTrack == true) "On Track" else if (p != null) "Behind Schedule" else "Unknown",
                        valueColor = if (p?.isOnTrack == true) PrimaryGreen else AccentRed
                    )
                    if (p?.estimatedCompletionDate != null) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFF2D333B))
                        DetailRow(
                            label = "Est. Completion",
                            value = dateFormat.format(Date(p.estimatedCompletionDate))
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            ModernCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Add Progress", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = viewModel.addAmount,
                        onValueChange = { viewModel.addAmount = it },
                        label = { Text("Amount to add") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { viewModel.addToSuccess {} },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        enabled = viewModel.addAmount.toDoubleOrNull() != null && (viewModel.addAmount.toDoubleOrNull() ?: 0.0) > 0,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                    ) {
                        Text("Update Progress", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onBackground
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
        Text(value, color = valueColor, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

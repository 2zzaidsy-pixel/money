package com.paywise.app.ui.screens.goals

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
import androidx.compose.ui.graphics.Color
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
class AddGoalViewModel @Inject constructor(
    private val repository: PayWiseRepository,
    private val preferencesManager: PreferencesManager,
    private val firebaseService: FirebaseService
) : ViewModel() {

    var title by mutableStateOf("")
    var goalType by mutableStateOf(GoalType.CUSTOM)
    var targetAmount by mutableStateOf("")
    var deadline by mutableStateOf("")
    var priority by mutableStateOf(1)
    var isLoading by mutableStateOf(false)

    fun save(onSaved: () -> Unit) {
        viewModelScope.launch {
            isLoading = true
            try {
                val userId = preferencesManager.userId.first() ?: return@launch
                val amount = targetAmount.toDoubleOrNull() ?: return@launch
                val deadlineMillis = parseDeadline(deadline) ?: return@launch

                val monthsToDeadline = ((deadlineMillis - System.currentTimeMillis()) / (1000 * 60 * 60 * 24 * 30)).toInt().coerceAtLeast(1)
                val monthlySaving = amount / monthsToDeadline

                val goal = FinancialGoal(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    title = title.ifBlank { goalType.displayName },
                    type = goalType,
                    targetAmount = amount,
                    deadline = deadlineMillis,
                    priority = priority,
                    monthlySavingRequired = monthlySaving
                )
                repository.addGoal(goal)
                firebaseService.syncGoal(goal)
                onSaved()
            } finally {
                isLoading = false
            }
        }
    }

    private fun parseDeadline(dateStr: String): Long? {
        return try {
            val parts = dateStr.split("/")
            if (parts.size == 2) {
                val cal = Calendar.getInstance()
                cal.set(Calendar.MONTH, parts[0].toInt() - 1)
                cal.set(Calendar.YEAR, parts[1].toInt())
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                cal.timeInMillis
            } else null
        } catch (e: Exception) { null }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddGoalScreen(
    onSaved: () -> Unit,
    onBack: () -> Unit,
    viewModel: AddGoalViewModel = hiltViewModel()
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("New Goal", fontWeight = FontWeight.Bold) },
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
            Text("Goal Type", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GoalType.entries.take(4).forEach { type ->
                    FilterChip(
                        selected = viewModel.goalType == type,
                        onClick = { viewModel.goalType = type },
                        label = { Text(type.displayName, style = MaterialTheme.typography.bodySmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryGreen.copy(alpha = 0.2f),
                            selectedLabelColor = PrimaryGreen
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GoalType.entries.drop(4).forEach { type ->
                    FilterChip(
                        selected = viewModel.goalType == type,
                        onClick = { viewModel.goalType = type },
                        label = { Text(type.displayName, style = MaterialTheme.typography.bodySmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryGreen.copy(alpha = 0.2f),
                            selectedLabelColor = PrimaryGreen
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = viewModel.title,
                onValueChange = { viewModel.title = it },
                label = { Text("Goal Title") },
                placeholder = { Text("e.g., Buy a Toyota Camry") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = viewModel.targetAmount,
                onValueChange = { viewModel.targetAmount = it },
                label = { Text("Target Amount") },
                leadingIcon = { Text("$", fontWeight = FontWeight.Bold) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = viewModel.deadline,
                onValueChange = { viewModel.deadline = it },
                label = { Text("Deadline (MM/YYYY)") },
                placeholder = { Text("e.g., 12/2026") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text("Priority", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (1..3).forEach { p ->
                    FilterChip(
                        selected = viewModel.priority == p,
                        onClick = { viewModel.priority = p },
                        label = { Text(if (p == 1) "High" else if (p == 2) "Medium" else "Low") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryGreen.copy(alpha = 0.2f),
                            selectedLabelColor = PrimaryGreen
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { viewModel.save(onSaved) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = !viewModel.isLoading && viewModel.targetAmount.toDoubleOrNull() != null && viewModel.deadline.isNotBlank(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
            ) {
                if (viewModel.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("Create Goal", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

package com.paywise.app.ui.screens.simulator

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
import com.paywise.app.data.local.PreferencesManager
import com.paywise.app.data.repository.PayWiseRepository
import com.paywise.app.domain.model.*
import com.paywise.app.ui.components.ModernCard
import com.paywise.app.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SimulatorViewModel @Inject constructor(
    private val repository: PayWiseRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    var simulationType by mutableStateOf(SimulationType.LOAN)
    var monthlyCost by mutableStateOf("")
    var oneTimeCost by mutableStateOf("")
    var scenarioTitle by mutableStateOf("")
    var result by mutableStateOf<SimulationResult?>(null)
    var isLoading by mutableStateOf(false)
    var currentSalary by mutableStateOf(0.0)
    var currentHealthScore by mutableStateOf(0)

    init {
        loadCurrentData()
    }

    private fun loadCurrentData() {
        viewModelScope.launch {
            val userId = preferencesManager.userId.first() ?: return@launch
            val user = repository.getUserOnce(userId)
            currentSalary = user?.salaryAmount ?: 0.0
            val health = repository.calculateHealthScore(userId)
            currentHealthScore = health.score
        }
    }

    fun runSimulation() {
        viewModelScope.launch {
            isLoading = true
            val userId = preferencesManager.userId.first() ?: return@launch
            val monthly = monthlyCost.toDoubleOrNull() ?: 0.0
            val oneTime = oneTimeCost.toDoubleOrNull() ?: 0.0

            result = repository.simulateFinancialDecision(userId, monthly, oneTime, simulationType)

            val sim = Simulation(
                userId = userId,
                type = simulationType,
                title = scenarioTitle.ifBlank { simulationType.name },
                monthlyCost = monthly,
                oneTimeCost = oneTime
            )
            repository.addSimulation(sim)
            isLoading = false
        }
    }

    fun applyPreset(type: SimulationType) {
        simulationType = type
        when (type) {
            SimulationType.LOAN -> {
                scenarioTitle = "Car Loan"
                monthlyCost = "700"
                oneTimeCost = ""
            }
            SimulationType.RENT_INCREASE -> {
                scenarioTitle = "New Apartment Rent"
                monthlyCost = "2000"
                oneTimeCost = ""
            }
            SimulationType.TRAVEL -> {
                scenarioTitle = "Travel"
                monthlyCost = ""
                oneTimeCost = "5000"
            }
            SimulationType.CUSTOM -> {
                scenarioTitle = ""
                monthlyCost = ""
                oneTimeCost = ""
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimulatorScreen(
    onBack: () -> Unit,
    viewModel: SimulatorViewModel = hiltViewModel()
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Financial Simulator", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                text = "Simulate financial decisions before making them",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Scenario presets
            Text("Choose a scenario", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ScenarioPresetChip("Car Loan", Icons.Default.DirectionsCar, viewModel.simulationType == SimulationType.LOAN) {
                    viewModel.applyPreset(SimulationType.LOAN)
                }
                ScenarioPresetChip("Rent Increase", Icons.Default.Home, viewModel.simulationType == SimulationType.RENT_INCREASE) {
                    viewModel.applyPreset(SimulationType.RENT_INCREASE)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ScenarioPresetChip("Travel", Icons.Default.Flight, viewModel.simulationType == SimulationType.TRAVEL) {
                    viewModel.applyPreset(SimulationType.TRAVEL)
                }
                ScenarioPresetChip("Custom", Icons.Default.Edit, viewModel.simulationType == SimulationType.CUSTOM) {
                    viewModel.applyPreset(SimulationType.CUSTOM)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Input fields
            OutlinedTextField(
                value = viewModel.scenarioTitle,
                onValueChange = { viewModel.scenarioTitle = it },
                label = { Text("Scenario Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = viewModel.monthlyCost,
                onValueChange = { viewModel.monthlyCost = it },
                label = { Text("Monthly Commitment") },
                leadingIcon = { Text("$", fontWeight = FontWeight.Bold) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = viewModel.oneTimeCost,
                onValueChange = { viewModel.oneTimeCost = it },
                label = { Text("One-time Cost") },
                leadingIcon = { Text("$", fontWeight = FontWeight.Bold) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Run button
            Button(
                onClick = { viewModel.runSimulation() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = !viewModel.isLoading && (viewModel.monthlyCost.isNotBlank() || viewModel.oneTimeCost.isNotBlank()),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
            ) {
                Icon(Icons.Default.Science, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Run Simulation", fontWeight = FontWeight.SemiBold)
            }

            // Results
            viewModel.result?.let { result ->
                Spacer(modifier = Modifier.height(24.dp))
                Text("Results", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))

                ModernCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        ResultRow("Current Balance", "${String.format("%.0f", viewModel.currentSalary)}")
                        ResultRow("New Remaining", "${String.format("%.0f", result.newRemainingBalance)}",
                            if (result.newRemainingBalance > 0) PrimaryGreen else AccentRed)
                        Divider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFF2D333B))
                        ResultRow("Savings Reduction", "${String.format("%.1f", result.savingsReductionPercent)}%", AccentOrange)
                        ResultRow("Health Score Impact", "-${result.healthScoreImpact} points", AccentRed)
                        ResultRow("New Health Score", "${(viewModel.currentHealthScore - result.healthScoreImpact).coerceAtLeast(0)}",
                            if (viewModel.currentHealthScore - result.healthScoreImpact > 50) PrimaryGreen else AccentOrange)

                        if (result.goalDelays.isNotEmpty()) {
                            Divider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFF2D333B))
                            Text("Goal Delays", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = AccentRed)
                            result.goalDelays.forEach { delay ->
                                Text(
                                    text = "${delay.goalTitle}: +${delay.additionalMonths} months",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Recommendation
                ModernCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Recommendation", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        val recommendation = when {
                            result.healthScoreImpact > 20 -> "This decision significantly impacts your financial health. Consider alternatives."
                            result.healthScoreImpact > 10 -> "Moderate impact. Proceed with caution."
                            result.goalDelays.isNotEmpty() -> "This will delay your goals. Consider adjusting the amounts."
                            else -> "This decision has minimal impact on your finances."
                        }
                        Text(recommendation, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
fun ScenarioPresetChip(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.weight(1f),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) PrimaryGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
        border = if (isSelected) BorderStroke(1.dp, PrimaryGreen) else null
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = if (isSelected) PrimaryGreen else TextSecondary, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.bodySmall, color = if (isSelected) PrimaryGreen else MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
fun ResultRow(label: String, value: String, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = valueColor)
    }
}

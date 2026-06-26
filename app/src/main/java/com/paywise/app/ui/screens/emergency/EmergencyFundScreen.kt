package com.paywise.app.ui.screens.emergency

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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.paywise.app.data.local.PreferencesManager
import com.paywise.app.data.repository.PayWiseRepository
import com.paywise.app.domain.model.*
import com.paywise.app.ui.components.AnimatedCircularProgress
import com.paywise.app.ui.components.ModernCard
import com.paywise.app.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EmergencyFundViewModel @Inject constructor(
    private val repository: PayWiseRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val userId = MutableStateFlow("")
    val fund = userId.flatMapLatest { uid ->
        if (uid.isBlank()) flowOf(null) else repository.getEmergencyFund(uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    var currentAmount by mutableStateOf("")
    var targetMonths by mutableStateOf(6)
    var monthlyExpenses by mutableStateOf(0.0)
    var coverageMonths by mutableStateOf(0.0)
    var isLoading by mutableStateOf(false)

    init {
        viewModelScope.launch {
            preferencesManager.userId.collectLatest { uid ->
                if (!uid.isNullOrBlank()) {
                    userId.value = uid
                    calculateCoverage(uid)
                }
            }
        }
    }

    private fun calculateCoverage(uid: String) {
        viewModelScope.launch {
            val fundVal = repository.getEmergencyFundOnce(uid)
            currentAmount = fundVal?.currentAmount?.toString() ?: "0"
            monthlyExpenses = repository.calculateSalaryInfo(uid).totalSpent
            coverageMonths = if (monthlyExpenses > 0) (fundVal?.currentAmount ?: 0.0) / monthlyExpenses else 0.0
        }
    }

    fun save() {
        viewModelScope.launch {
            isLoading = true
            val uid = userId.first() ?: return@launch
            val amount = currentAmount.toDoubleOrNull() ?: 0.0
            val existing = repository.getEmergencyFundOnce(uid)
            val fund = EmergencyFund(
                id = existing?.id ?: java.util.UUID.randomUUID().toString(),
                userId = uid,
                currentAmount = amount,
                targetMonths = targetMonths
            )
            repository.saveEmergencyFund(fund)
            calculateCoverage(uid)
            isLoading = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyFundScreen(
    onBack: () -> Unit,
    viewModel: EmergencyFundViewModel = hiltViewModel()
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Emergency Fund", fontWeight = FontWeight.Bold) },
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
            ModernCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val coverageLevel = when {
                        viewModel.coverageMonths >= 6 -> "Strong"
                        viewModel.coverageMonths >= 3 -> "Moderate"
                        else -> "Low"
                    }
                    val coverageColor = when {
                        viewModel.coverageMonths >= 6 -> PrimaryGreen
                        viewModel.coverageMonths >= 3 -> AccentOrange
                        else -> AccentRed
                    }

                    AnimatedCircularProgress(
                        percentage = (viewModel.coverageMonths.toFloat() / 12f).coerceIn(0f, 1f),
                        size = 140.dp,
                        strokeWidth = 12.dp,
                        progressColor = coverageColor,
                        value = "${String.format("%.1f", viewModel.coverageMonths)} months",
                        label = coverageLevel
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Monthly Expenses: ${String.format("%.0f", viewModel.monthlyExpenses)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = viewModel.currentAmount,
                onValueChange = { viewModel.currentAmount = it },
                label = { Text("Current Emergency Savings") },
                leadingIcon = { Text("$", fontWeight = FontWeight.Bold) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text("Target Coverage", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(3, 6, 9, 12).forEach { months ->
                    FilterChip(
                        selected = viewModel.targetMonths == months,
                        onClick = { viewModel.targetMonths = months },
                        label = { Text("$months months") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryGreen.copy(alpha = 0.2f),
                            selectedLabelColor = PrimaryGreen
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { viewModel.save() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
            ) {
                Text("Save", fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(24.dp))

            ModernCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Tips", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    TipRow("Aim for 3-6 months of essential expenses")
                    TipRow("Keep emergency fund in a separate savings account")
                    TipRow("Only use for genuine emergencies")
                    TipRow("Replenish after any withdrawal")
                }
            }
        }
    }
}

@Composable
fun TipRow(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
    }
}

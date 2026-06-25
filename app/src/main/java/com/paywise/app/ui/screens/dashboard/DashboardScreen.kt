package com.paywise.app.ui.screens.dashboard

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
<<<<<<< HEAD
import androidx.compose.ui.graphics.vector.ImageVector
=======
>>>>>>> 7a0db4f9dfd618cf59f2e480738276d42bb5c2a5
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.paywise.app.data.local.PreferencesManager
import com.paywise.app.data.repository.PayWiseRepository
import com.paywise.app.domain.model.*
import com.paywise.app.ui.components.*
import com.paywise.app.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class DashboardUiState(
    val isLoading: Boolean = true,
    val userId: String = "",
    val salaryInfo: SalaryInfo? = null,
    val healthScore: FinancialHealthScore? = null,
    val survivalDateInfo: SurvivalDateInfo? = null,
    val prediction: MonthlyPrediction? = null,
    val leaks: List<MoneyLeak> = emptyList(),
    val currency: String = "SAR",
    val userName: String = ""
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: PayWiseRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            preferencesManager.userId.collectLatest { userId ->
                if (userId.isNullOrEmpty()) return@collectLatest
                _uiState.update { it.copy(userId = userId) }

                repository.getUser(userId).collectLatest { user ->
                    if (user == null) return@collectLatest
                    _uiState.update { it.copy(userName = user.name, currency = user.currency) }

                    val salaryInfo = repository.calculateSalaryInfo(userId)
                    val healthScore = repository.calculateHealthScore(userId)
                    val survivalDate = repository.calculateSurvivalDate(userId)
                    val prediction = repository.calculateMonthlyPrediction(userId)
                    val leaks = repository.detectMoneyLeaks(userId)

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            salaryInfo = salaryInfo,
                            healthScore = healthScore,
                            survivalDateInfo = survivalDate,
                            prediction = prediction,
                            leaks = leaks
                        )
                    }
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            val userId = _uiState.value.userId
            if (userId.isBlank()) return@launch
            _uiState.update { it.copy(isLoading = true) }
            delay(500)

            val salaryInfo = repository.calculateSalaryInfo(userId)
            val healthScore = repository.calculateHealthScore(userId)
            val survivalDate = repository.calculateSurvivalDate(userId)
            val prediction = repository.calculateMonthlyPrediction(userId)
            val leaks = repository.detectMoneyLeaks(userId)

            _uiState.update {
                it.copy(
                    isLoading = false,
                    salaryInfo = salaryInfo,
                    healthScore = healthScore,
                    survivalDateInfo = survivalDate,
                    prediction = prediction,
                    leaks = leaks
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToExpenses: () -> Unit,
    onNavigateToBudgets: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToGoals: () -> Unit,
    onNavigateToSimulator: () -> Unit,
    onNavigateToEmergencyFund: () -> Unit,
    onNavigateToSubscriptions: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToPremium: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 80.dp)
        ) {
            // Header
            ModernCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                containerColor = PrimaryGreen
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Good ${getTimeOfDay()},",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            Text(
                                text = uiState.userName.ifEmpty { "User" },
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Row {
                            IconButton(onClick = onNavigateToPremium) {
                                Icon(
                                    Icons.Default.Star,
                                    contentDescription = "Premium",
                                    tint = Color.White
                                )
                            }
                            IconButton(onClick = onNavigateToProfile) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = "Profile",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            }

            if (uiState.isLoading) {
                repeat(4) {
                    SkeletonCard(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                }
            } else {
                // Salary Card
                uiState.salaryInfo?.let { salary ->
                    ModernCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Salary Overview",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(
                                    Icons.Default.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = PrimaryGreen
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${String.format("%.0f", salary.salary)} ${uiState.currency}",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryGreen
                                    )
                                    Text(
                                        text = "Salary",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${String.format("%.0f", salary.totalSpent)} ${uiState.currency}",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = AccentOrange
                                    )
                                    Text(
                                        text = "Spent",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${String.format("%.0f", salary.remaining)} ${uiState.currency}",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (salary.remaining > 0) AccentBlue else AccentRed
                                    )
                                    Text(
                                        text = "Remaining",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            SalaryProgressBar(percentage = salary.percentageUsed)
                            Text(
                                text = "${(salary.percentageUsed * 100).toInt()}% consumed",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }

                // Financial Health Score
                uiState.healthScore?.let { health ->
                    ModernCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HealthScoreGauge(score = health.score)
                            Spacer(modifier = Modifier.width(24.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Financial Health",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                StatItem(
                                    label = "Saving Ratio",
                                    value = "${(health.savingRatio * 100).toInt()}%"
                                )
                                StatItem(
                                    label = "Budget Compliance",
                                    value = "${(health.budgetCompliance * 100).toInt()}%"
                                )
                                StatItem(
                                    label = "Essential Expenses",
                                    value = "${(health.essentialRatio * 100).toInt()}%"
                                )
                            }
                        }
                    }
                }

                // Survival Date & Prediction
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    uiState.survivalDateInfo?.let { survival ->
                        ModernCard(
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Icon(
                                    Icons.Default.Event,
                                    contentDescription = null,
                                    tint = AccentBlue,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Survival Date",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextSecondary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                if (survival.survivalDate != null) {
                                    Text(
                                        text = formatDate(survival.survivalDate),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                } else if (survival.remainingBeforeNextSalary != null) {
                                    Text(
                                        text = "${String.format("%.0f", survival.remainingBeforeNextSalary)} ${uiState.currency} remaining",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (survival.remainingBeforeNextSalary > 0) PrimaryGreen else AccentRed
                                    )
                                }
                            }
                        }
                    }

                    uiState.prediction?.let { pred ->
                        ModernCard(
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Icon(
                                    Icons.Default.TrendingUp,
                                    contentDescription = null,
                                    tint = if (pred.comparisonToLastMonth > 0) AccentOrange else PrimaryGreen,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Prediction",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextSecondary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (pred.comparisonToLastMonth > 0)
                                        "${pred.comparisonToLastMonth.toInt()}% more than last month"
                                    else
                                        "${(-pred.comparisonToLastMonth).toInt()}% less than last month",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (pred.comparisonToLastMonth > 0) AccentOrange else PrimaryGreen
                                )
                                Text(
                                    text = "Save ~${String.format("%.0f", pred.predictedSaving)} ${uiState.currency}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }

                // Money Leaks
                if (uiState.leaks.isNotEmpty()) {
                    ModernCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = AccentOrange,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Money Leaks Detected",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            uiState.leaks.forEach { leak ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    color = AccentOrange.copy(alpha = 0.1f)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = "${leak.category}: ${String.format("%.0f", leak.monthlyCost)} ${uiState.currency}/month",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = AccentOrange
                                        )
                                        Text(
                                            text = "= ${String.format("%.0f", leak.annualCost)} ${uiState.currency}/year",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary
                                        )
                                        Text(
                                            text = leak.suggestion,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Quick Actions
                Text(
                    text = "Quick Actions",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionCard(
                        icon = Icons.Default.Receipt,
                        label = "Expenses",
                        color = AccentRed,
                        onClick = onNavigateToExpenses,
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionCard(
                        icon = Icons.Default.AccountTree,
                        label = "Budgets",
                        color = AccentOrange,
                        onClick = onNavigateToBudgets,
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionCard(
                        icon = Icons.Default.BarChart,
                        label = "Reports",
                        color = AccentBlue,
                        onClick = onNavigateToReports,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionCard(
                        icon = Icons.Default.Flag,
                        label = "Goals",
                        color = AccentPurple,
                        onClick = onNavigateToGoals,
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionCard(
                        icon = Icons.Default.Science,
                        label = "Simulator",
                        color = PrimaryGreen,
                        onClick = onNavigateToSimulator,
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionCard(
                        icon = Icons.Default.Savings,
                        label = "Emergency",
                        color = AccentBlue,
                        onClick = onNavigateToEmergencyFund,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionCard(
                        icon = Icons.Default.Subscriptions,
                        label = "Subscriptions",
                        color = AccentOrange,
                        onClick = onNavigateToSubscriptions,
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionCard(
                        icon = Icons.Default.Settings,
                        label = "Settings",
                        color = TextSecondary,
                        onClick = onNavigateToSettings,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Refresh FAB
        FloatingActionButton(
            onClick = { viewModel.refresh() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
            containerColor = PrimaryGreen,
            contentColor = Color.White
        ) {
            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
        }
    }
}

@Composable
fun QuickActionCard(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = color.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        contentDescription = label,
                        tint = color,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun getTimeOfDay(): String {
    val cal = Calendar.getInstance()
    val hour = cal.get(Calendar.HOUR_OF_DAY)
    return when {
        hour < 12 -> "Morning"
        hour < 17 -> "Afternoon"
        else -> "Evening"
    }
}

private fun formatDate(millis: Long): String {
    val sdf = SimpleDateFormat("MMM dd", Locale.getDefault())
    return sdf.format(Date(millis))
}

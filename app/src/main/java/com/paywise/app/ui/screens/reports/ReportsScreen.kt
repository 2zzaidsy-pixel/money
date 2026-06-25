package com.paywise.app.ui.screens.reports

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.paywise.app.data.local.CategoryTotal
import com.paywise.app.data.local.PreferencesManager
import com.paywise.app.data.repository.PayWiseRepository
import com.paywise.app.domain.model.*
import com.paywise.app.ui.components.HealthScoreGauge
import com.paywise.app.ui.components.ModernCard
import com.paywise.app.ui.screens.expenses.getCategoryColor
import com.paywise.app.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

enum class ReportPeriod { WEEKLY, MONTHLY, YEARLY }

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val repository: PayWiseRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val userId = MutableStateFlow("")
    var selectedPeriod by mutableStateOf(ReportPeriod.MONTHLY)
    var categoryTotals by mutableStateOf<List<CategoryTotal>>(emptyList())
    var healthScore by mutableStateOf<FinancialHealthScore?>(null)
    var totalSpent by mutableStateOf(0.0)
    var comparisonText by mutableStateOf("")
    var savingsRatio by mutableStateOf(0.0)

    init {
        viewModelScope.launch {
            preferencesManager.userId.collectLatest { uid ->
                if (!uid.isNullOrBlank()) {
                    userId.value = uid
                    loadReport()
                }
            }
        }
    }

    fun loadReport() {
        viewModelScope.launch {
            val uid = userId.first() ?: return@launch
            val calendar = Calendar.getInstance()
            val (startDate, endDate) = when (selectedPeriod) {
                ReportPeriod.WEEKLY -> {
                    calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                    calendar.set(Calendar.HOUR_OF_DAY, 0)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                    val start = calendar.timeInMillis
                    calendar.add(Calendar.DAY_OF_WEEK, 6)
                    calendar.set(Calendar.HOUR_OF_DAY, 23)
                    calendar.set(Calendar.MINUTE, 59)
                    start to calendar.timeInMillis
                }
                ReportPeriod.MONTHLY -> {
                    calendar.set(Calendar.DAY_OF_MONTH, 1)
                    calendar.set(Calendar.HOUR_OF_DAY, 0)
                    val start = calendar.timeInMillis
                    calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                    calendar.set(Calendar.HOUR_OF_DAY, 23)
                    start to calendar.timeInMillis
                }
                ReportPeriod.YEARLY -> {
                    calendar.set(Calendar.DAY_OF_YEAR, 1)
                    calendar.set(Calendar.HOUR_OF_DAY, 0)
                    val start = calendar.timeInMillis
                    calendar.set(Calendar.DAY_OF_YEAR, calendar.getActualMaximum(Calendar.DAY_OF_YEAR))
                    calendar.set(Calendar.HOUR_OF_DAY, 23)
                    start to calendar.timeInMillis
                }
            }

            categoryTotals = repository.getCategoryTotals(uid, startDate, endDate).first()
            totalSpent = categoryTotals.sumOf { it.total }
            healthScore = repository.calculateHealthScore(uid)
            savingsRatio = if (totalSpent > 0) {
                val user = repository.getUserOnce(uid)
                if (user != null && user.salaryAmount > 0) (user.salaryAmount - totalSpent) / user.salaryAmount else 0.0
            } else 0.0

            // Comparison with previous period
            val prevEnd = startDate - 1
            val periodDuration = endDate - startDate
            val prevStart = prevEnd - periodDuration
            val prevTotal = repository.getTotalSpentBetweenOnce(uid, prevStart, prevEnd) ?: 0.0
            comparisonText = if (prevTotal > 0) {
                val change = ((totalSpent - prevTotal) / prevTotal) * 100
                if (change > 0) "${String.format("%.1f", change)}% increase from last period"
                else "${String.format("%.1f", -change)}% decrease from last period"
            } else "No previous data"
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    onBack: () -> Unit,
    viewModel: ReportsViewModel = hiltViewModel()
) {
    LaunchedEffect(viewModel.selectedPeriod) {
        viewModel.loadReport()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Reports", fontWeight = FontWeight.Bold) },
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
                .padding(16.dp)
        ) {
            // Period selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                ReportPeriod.entries.forEach { period ->
                    FilterChip(
                        selected = viewModel.selectedPeriod == period,
                        onClick = { viewModel.selectedPeriod = period },
                        label = { Text(period.name) },
                        modifier = Modifier.padding(horizontal = 4.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryGreen.copy(alpha = 0.2f),
                            selectedLabelColor = PrimaryGreen
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Total spent card
            ModernCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Total Spent",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = String.format("%.0f", viewModel.totalSpent),
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold,
                        color = AccentRed
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = viewModel.comparisonText,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (viewModel.comparisonText.contains("increase")) AccentRed else PrimaryGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Health score
            viewModel.healthScore?.let { health ->
                ModernCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HealthScoreGauge(score = health.score, size = 120.dp, strokeWidth = 10.dp)
                        Spacer(modifier = Modifier.width(20.dp))
                        Column {
                            Text("Financial Health", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Saving Ratio: ${(health.savingRatio * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
                            Text("Budget Compliance: ${(health.budgetCompliance * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
                            Text("Essential: ${(health.essentialRatio * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
                            Text("Emergency Fund: ${(health.emergencyFundStrength * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Category breakdown
            Text(
                text = "Spending by Category",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            if (viewModel.categoryTotals.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No spending data", color = TextSecondary)
                }
            } else {
                val maxTotal = viewModel.categoryTotals.maxOfOrNull { it.total } ?: 1.0
                viewModel.categoryTotals.sortedByDescending { it.total }.forEach { catTotal ->
                    val color = getCategoryColor(catTotal.category)
                    val percentage = (catTotal.total / maxTotal).toFloat().coerceIn(0f, 1f)
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    modifier = Modifier.size(8.dp),
                                    shape = CircleShape,
                                    color = color
                                ) {}
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(catTotal.category.displayName, style = MaterialTheme.typography.bodyMedium)
                            }
                            Text(
                                text = "${String.format("%.0f", catTotal.total)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .background(Color(0xFF2D333B), RoundedCornerShape(3.dp))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(fraction = percentage)
                                    .background(color, RoundedCornerShape(3.dp))
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Insights
            ModernCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Insights", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    val essentialTotal = viewModel.categoryTotals.filter { it.category.isEssential }.sumOf { it.total }
                    val nonEssentialTotal = viewModel.categoryTotals.filter { !it.category.isEssential }.sumOf { it.total }

                    InsightRow(
                        icon = Icons.Default.CheckCircle,
                        text = if (viewModel.savingsRatio > 0.2)
                            "Great! You're saving ${(viewModel.savingsRatio * 100).toInt()}% of your income"
                        else "Try to save at least 20% of your income",
                        color = if (viewModel.savingsRatio > 0.2) PrimaryGreen else AccentOrange
                    )
                    InsightRow(
                        icon = Icons.Default.Home,
                        text = if (essentialTotal / (viewModel.totalSpent.coerceAtLeast(1.0)) < 0.5)
                            "Essential expenses are ${(essentialTotal / viewModel.totalSpent.coerceAtLeast(1.0) * 100).toInt()}% of spending"
                        else "Essential expenses are high at ${(essentialTotal / viewModel.totalSpent.coerceAtLeast(1.0) * 100).toInt()}%",
                        color = if (essentialTotal / (viewModel.totalSpent.coerceAtLeast(1.0)) < 0.5) PrimaryGreen else AccentOrange
                    )
                    InsightRow(
                        icon = Icons.Default.TrendingDown,
                        text = if (nonEssentialTotal > 0)
                            "Non-essential spending: ${(nonEssentialTotal / viewModel.totalSpent.coerceAtLeast(1.0) * 100).toInt()}% of total"
                        else "No non-essential spending this period",
                        color = if (nonEssentialTotal > 0) AccentOrange else PrimaryGreen
                    )
                }
            }
        }
    }
}

@Composable
fun InsightRow(icon: ImageVector, text: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

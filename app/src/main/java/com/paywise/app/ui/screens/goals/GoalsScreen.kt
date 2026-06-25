package com.paywise.app.ui.screens.goals

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
import androidx.compose.ui.graphics.vector.ImageVector
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
class GoalsViewModel @Inject constructor(
    private val repository: PayWiseRepository,
    private val preferencesManager: PreferencesManager,
    private val firebaseService: FirebaseService
) : ViewModel() {

    private val userId = MutableStateFlow("")
    val goals = userId.flatMapLatest { uid ->
        if (uid.isBlank()) flowOf(emptyList()) else repository.getGoals(uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    var goalProgresses by mutableStateOf<List<GoalProgress>>(emptyList())
    var showDeleteDialog by mutableStateOf(false)
    var goalToDelete by mutableStateOf<FinancialGoal?>(null)

    init {
        viewModelScope.launch {
            preferencesManager.userId.collectLatest { uid ->
                if (!uid.isNullOrBlank()) userId.value = uid
            }
        }
        loadProgress()
    }

    fun loadProgress() {
        viewModelScope.launch {
            goalProgresses = goals.value.map { goal ->
                repository.calculateGoalProgress(goal)
            }
        }
    }

    fun confirmDelete(goal: FinancialGoal) {
        goalToDelete = goal
        showDeleteDialog = true
    }

    fun deleteGoal() {
        viewModelScope.launch {
            goalToDelete?.let { goal ->
                repository.deleteGoal(goal)
                showDeleteDialog = false
                goalToDelete = null
                loadProgress()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(
    onAddGoal: () -> Unit,
    onGoalClick: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: GoalsViewModel = hiltViewModel()
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Financial Goals", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                IconButton(onClick = onAddGoal) {
                    Icon(Icons.Default.Add, contentDescription = "Add Goal")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        )

        if (viewModel.goalProgresses.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Flag,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No goals yet", style = MaterialTheme.typography.titleMedium, color = TextSecondary)
                    Text("Set your first financial goal", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
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
                // Summary
                val totalSaved = viewModel.goalProgresses.sumOf { it.goal.currentAmount }
                val totalTarget = viewModel.goalProgresses.sumOf { it.goal.targetAmount }
                ModernCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Overall Progress", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "${String.format("%.0f", totalSaved)} / ${String.format("%.0f", totalTarget)}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryGreen
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        val overallPercent = if (totalTarget > 0) (totalSaved / totalTarget).toFloat() else 0f
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF2D333B))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(fraction = overallPercent.coerceIn(0f, 1f))
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(PrimaryGreen)
                            )
                        }
                    }
                }

                viewModel.goalProgresses.forEach { progress ->
                    GoalCard(
                        progress = progress,
                        onClick = { onGoalClick(progress.goal.id) },
                        onDelete = { viewModel.confirmDelete(progress.goal) }
                    )
                }
            }
        }
    }

    if (viewModel.showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.showDeleteDialog = false },
            title = { Text("Delete Goal") },
            text = { Text("Are you sure?") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteGoal() }) {
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
fun GoalCard(
    progress: GoalProgress,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM yyyy", Locale.getDefault()) }

    ModernCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        color = PrimaryGreen.copy(alpha = 0.1f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                getGoalIcon(progress.goal.type),
                                contentDescription = null,
                                tint = PrimaryGreen,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = progress.goal.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${String.format("%.0f", progress.goal.targetAmount)} target",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
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
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF2D333B))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction = progress.percentageComplete)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(PrimaryGreen, AccentBlue)
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
                    text = "${(progress.percentageComplete * 100).toInt()}% complete",
                    style = MaterialTheme.typography.bodySmall,
                    color = PrimaryGreen,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${String.format("%.0f", progress.goal.currentAmount)} / ${String.format("%.0f", progress.goal.targetAmount)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            if (progress.goal.monthlySavingRequired > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Save ${String.format("%.0f", progress.goal.monthlySavingRequired)}/month",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (progress.isOnTrack) PrimaryGreen else AccentOrange
                )
            }
        }
    }
}

fun getGoalIcon(type: GoalType): ImageVector = when (type) {
    GoalType.CAR -> Icons.Default.DirectionsCar
    GoalType.MARRIAGE -> Icons.Default.Favorite
    GoalType.TRAVEL -> Icons.Default.Flight
    GoalType.HOUSE -> Icons.Default.House
    GoalType.EDUCATION -> Icons.Default.School
    GoalType.NEW_PHONE -> Icons.Default.PhoneAndroid
    GoalType.CUSTOM -> Icons.Default.Flag
}

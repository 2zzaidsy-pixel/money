package com.paywise.app.ui.screens.subscriptions

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paywise.app.data.local.PreferencesManager
import com.paywise.app.data.repository.PayWiseRepository
import com.paywise.app.domain.model.Subscription
import com.paywise.app.ui.components.ModernCard
import androidx.compose.ui.graphics.Color
import com.paywise.app.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SubscriptionsViewModel @Inject constructor(
    private val repository: PayWiseRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val userId = MutableStateFlow("")
    val subscriptions = userId.flatMapLatest { uid ->
        if (uid.isBlank()) flowOf(emptyList()) else repository.getSubscriptions(uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    var showAddDialog by mutableStateOf(false)
    var newName by mutableStateOf("")
    var newMonthlyCost by mutableStateOf("")

    init {
        viewModelScope.launch {
            preferencesManager.userId.collectLatest { uid ->
                if (!uid.isNullOrBlank()) userId.value = uid
            }
        }
    }

    fun toggleSubscription(id: String, isActive: Boolean) {
        viewModelScope.launch {
            repository.toggleSubscription(id, isActive)
        }
    }

    fun addSubscription() {
        viewModelScope.launch {
            val uid = userId.first() ?: return@launch
            val cost = newMonthlyCost.toDoubleOrNull() ?: return@launch
            val sub = Subscription(
                userId = uid,
                name = newName,
                monthlyCost = cost,
                annualCost = cost * 12
            )
            repository.addSubscription(sub)
            showAddDialog = false
            newName = ""
            newMonthlyCost = ""
        }
    }

    fun deleteSubscription(sub: Subscription) {
        viewModelScope.launch {
            repository.deleteSubscription(sub)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionsScreen(
    onBack: () -> Unit,
    viewModel: SubscriptionsViewModel = hiltViewModel()
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Subscriptions", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                IconButton(onClick = { viewModel.showAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Subscription")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
        )

        if (viewModel.subscriptions.value.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Subscriptions, contentDescription = null, modifier = Modifier.size(64.dp), tint = TextSecondary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No subscriptions tracked", style = MaterialTheme.typography.titleMedium, color = TextSecondary)
                    Text("Tap + to add one", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val totalMonthly = viewModel.subscriptions.value.filter { it.isActive }.sumOf { it.monthlyCost }
                val totalAnnual = viewModel.subscriptions.value.filter { it.isActive }.sumOf { it.annualCost }

                ModernCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = String.format("%.0f", totalMonthly),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = AccentOrange
                            )
                            Text("/month", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = String.format("%.0f", totalAnnual),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = AccentRed
                            )
                            Text("/year", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                    }
                }

                viewModel.subscriptions.value.forEach { sub ->
                    SubscriptionCard(
                        subscription = sub,
                        onToggle = { viewModel.toggleSubscription(sub.id, !sub.isActive) },
                        onDelete = { viewModel.deleteSubscription(sub) }
                    )
                }
            }
        }
    }

    if (viewModel.showAddDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.showAddDialog = false },
            title = { Text("Add Subscription") },
            text = {
                Column {
                    OutlinedTextField(
                        value = viewModel.newName,
                        onValueChange = { viewModel.newName = it },
                        label = { Text("Name") },
                        placeholder = { Text("e.g., Netflix, Spotify") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = viewModel.newMonthlyCost,
                        onValueChange = { viewModel.newMonthlyCost = it },
                        label = { Text("Monthly Cost") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.addSubscription() },
                    enabled = viewModel.newName.isNotBlank() && viewModel.newMonthlyCost.toDoubleOrNull() != null
                ) { Text("Save", color = PrimaryGreen) }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.showAddDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun SubscriptionCard(
    subscription: Subscription,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    ModernCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = if (subscription.isActive) AccentOrange.copy(alpha = 0.1f) else Color(0xFF2D333B)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Subscriptions, contentDescription = null, tint = if (subscription.isActive) AccentOrange else TextSecondary, modifier = Modifier.size(22.dp))
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(subscription.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    text = "${String.format("%.0f", subscription.monthlyCost)}/month - ${String.format("%.0f", subscription.annualCost)}/year",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            Switch(
                checked = subscription.isActive,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(checkedTrackColor = PrimaryGreen)
            )

            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AccentRed.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
            }
        }
    }
}


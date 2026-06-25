package com.paywise.app.ui.screens.settings

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.paywise.app.data.local.PreferencesManager
import com.paywise.app.data.repository.PayWiseRepository
import com.paywise.app.domain.model.UserProfile
import com.paywise.app.firebase.FirebaseService
import com.paywise.app.ui.components.ModernCard
import com.paywise.app.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val repository: PayWiseRepository,
    private val firebaseService: FirebaseService
) : ViewModel() {

    val isDarkMode = preferencesManager.isDarkMode
    val language = preferencesManager.currentLanguage
    val currency = preferencesManager.currentCurrency
    val notificationsEnabled = preferencesManager.notificationsEnabled
    val budgetAlerts = preferencesManager.budgetAlerts

    var showLanguageDialog by mutableStateOf(false)
    var showCurrencyDialog by mutableStateOf(false)

    fun toggleDarkMode(enabled: Boolean) {
        viewModelScope.launch { preferencesManager.setDarkMode(enabled) }
    }

    fun setLanguage(lang: String) {
        viewModelScope.launch {
            preferencesManager.setLanguage(lang)
            val uid = preferencesManager.userId.first() ?: return@launch
            repository.updateLanguage(uid, lang)
            showLanguageDialog = false
        }
    }

    fun setCurrency(curr: String) {
        viewModelScope.launch {
            preferencesManager.setCurrency(curr)
            showCurrencyDialog = false
        }
    }

    fun toggleNotifications(enabled: Boolean) {
        viewModelScope.launch { preferencesManager.setNotificationsEnabled(enabled) }
    }

    fun toggleBudgetAlerts(enabled: Boolean) {
        viewModelScope.launch { preferencesManager.setBudgetAlerts(enabled) }
    }

    fun signOut(onSignedOut: () -> Unit) {
        viewModelScope.launch {
            firebaseService.signOut()
            preferencesManager.setLoggedOut()
            onSignedOut()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    darkTheme: Boolean,
    onThemeChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    onPrivacy: () -> Unit = {},
    onTerms: () -> Unit = {},
    onSupport: () -> Unit = {},
    onPremium: () -> Unit = {},
    onSignOut: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle(initialValue = false)
    val currentLang by viewModel.language.collectAsStateWithLifecycle(initialValue = "en")
    val currentCurrency by viewModel.currency.collectAsStateWithLifecycle(initialValue = "SAR")
    val notifEnabled by viewModel.notificationsEnabled.collectAsStateWithLifecycle(initialValue = true)
    val alertsEnabled by viewModel.budgetAlerts.collectAsStateWithLifecycle(initialValue = true)

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Settings", fontWeight = FontWeight.Bold) },
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
            // Appearance Section
            Text("Appearance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
            ModernCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(8.dp)) {
                    SettingsRow(
                        icon = Icons.Default.DarkMode,
                        title = "Dark Mode",
                        subtitle = "Toggle dark theme",
                        trailing = {
                            Switch(checked = isDarkMode, onCheckedChange = { onThemeChange(it) }, colors = SwitchDefaults.colors(checkedTrackColor = PrimaryGreen))
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFF2D333B))
                    SettingsRow(
                        icon = Icons.Default.Language,
                        title = "Language",
                        subtitle = if (currentLang == "en") "English" else "العربية",
                        onClick = { viewModel.showLanguageDialog = true }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFF2D333B))
                    SettingsRow(
                        icon = Icons.Default.AttachMoney,
                        title = "Currency",
                        subtitle = currentCurrency,
                        onClick = { viewModel.showCurrencyDialog = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Notifications
            Text("Notifications", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
            ModernCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(8.dp)) {
                    SettingsRow(
                        icon = Icons.Default.Notifications,
                        title = "Push Notifications",
                        subtitle = "Receive alerts and insights",
                        trailing = {
                            Switch(checked = notifEnabled, onCheckedChange = { viewModel.toggleNotifications(it) }, colors = SwitchDefaults.colors(checkedTrackColor = PrimaryGreen))
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFF2D333B))
                    SettingsRow(
                        icon = Icons.Default.Warning,
                        title = "Budget Alerts",
                        subtitle = "Get notified when budget is exceeded",
                        trailing = {
                            Switch(checked = alertsEnabled, onCheckedChange = { viewModel.toggleBudgetAlerts(it) }, colors = SwitchDefaults.colors(checkedTrackColor = PrimaryGreen))
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Account
            Text("Account", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
            ModernCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(8.dp)) {
                    SettingsRow(
                        icon = Icons.Default.CloudSync,
                        title = "Backup & Sync",
                        subtitle = "Sync data across devices",
                        onClick = { }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFF2D333B))
                    SettingsRow(
                        icon = Icons.Default.Star,
                        title = "Premium",
                        subtitle = "Unlock all features",
                        onClick = onPremium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // About
            Text("About", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
            ModernCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(8.dp)) {
                    SettingsRow(
                        icon = Icons.Default.Policy,
                        title = "Privacy Policy",
                        subtitle = "How we handle your data",
                        onClick = onPrivacy
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFF2D333B))
                    SettingsRow(
                        icon = Icons.Default.Description,
                        title = "Terms of Service",
                        subtitle = "Usage terms and conditions",
                        onClick = onTerms
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFF2D333B))
                    SettingsRow(
                        icon = Icons.Default.QuestionAnswer,
                        title = "Help & Support",
                        subtitle = "FAQ, contact, and account deletion",
                        onClick = onSupport
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sign out
            Button(
                onClick = { viewModel.signOut {
                    onSignOut()
                } },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentRed.copy(alpha = 0.1f))
            ) {
                Icon(Icons.Default.Logout, contentDescription = null, tint = AccentRed)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sign Out", color = AccentRed, fontWeight = FontWeight.SemiBold)
            }
        }
    }

    // Language dialog
    if (viewModel.showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.showLanguageDialog = false },
            title = { Text("Select Language") },
            text = {
                Column {
                    listOf("en" to "English", "ar" to "العربية").forEach { (code, name) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setLanguage(code) }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = currentLang == code, onClick = { viewModel.setLanguage(code) })
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(name)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { viewModel.showLanguageDialog = false }) { Text("Cancel") } }
        )
    }

    // Currency dialog
    if (viewModel.showCurrencyDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.showCurrencyDialog = false },
            title = { Text("Select Currency") },
            text = {
                Column {
                    listOf("SAR", "USD", "EUR", "GBP", "AED", "QAR", "KWD", "BHD").forEach { curr ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setCurrency(curr) }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = currentCurrency == curr, onClick = { viewModel.setCurrency(curr) })
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(curr)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { viewModel.showCurrencyDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    trailing: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        if (trailing != null) {
            trailing()
        } else if (onClick != null) {
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary)
        }
    }
}


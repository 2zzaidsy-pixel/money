package com.paywise.app.ui.screens.legal

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyScreen(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Privacy Policy", fontWeight = FontWeight.Bold) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
        )
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)
        ) {
            Text("Last Updated: June 2026", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(16.dp))
            Text("""
1. Information We Collect

We collect information you provide directly: name, email address, financial data (salary, expenses, budgets, goals), and currency preferences.

2. How We Use Your Information

- To provide and improve our financial management services
- To calculate financial health scores and predictions
- To send personalized notifications and insights
- To sync your data across devices (if premium)

3. Data Storage and Security

Your financial data is encrypted at rest and in transit. We use industry-standard encryption protocols. Local data is encrypted using Android EncryptedSharedPreferences.

4. Data Sharing

We do not sell your personal information. We may share anonymized, aggregated data for analytics purposes.

5. Your Rights

You can request deletion of your account and all associated data at any time through the app settings.

6. Contact

For privacy concerns, contact: privacy@paywise.app
            """.trimIndent(), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

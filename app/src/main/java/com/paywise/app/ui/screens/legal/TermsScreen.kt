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
fun TermsScreen(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Terms of Service", fontWeight = FontWeight.Bold) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
        )
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)
        ) {
            Text("Last Updated: June 2026", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(16.dp))
            Text("""
1. Acceptance of Terms

By using PayWise, you agree to these terms of service.

2. Description of Service

PayWise is a personal financial management application that helps users track expenses, create budgets, set financial goals, and simulate financial decisions.

3. User Responsibilities

- Provide accurate financial information
- Maintain confidentiality of your account
- Use the app responsibly for personal financial management

4. Premium Subscription

Premium features require an active subscription. Subscriptions auto-renew unless cancelled. Refunds are handled per Google Play policy.

5. Limitation of Liability

PayWise provides financial tools and insights but does not provide professional financial advice. We are not responsible for financial decisions made based on app data.

6. Termination

We reserve the right to terminate accounts that violate these terms.

7. Changes

We may update these terms. Continued use after changes constitutes acceptance.
            """.trimIndent(), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

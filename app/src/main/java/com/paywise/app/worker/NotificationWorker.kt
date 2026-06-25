package com.paywise.app.worker

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.paywise.app.MainActivity
import com.paywise.app.R
import com.paywise.app.data.local.PreferencesManager
import com.paywise.app.data.repository.PayWiseRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class NotificationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: PayWiseRepository,
    private val preferencesManager: PreferencesManager
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val isEnabled = preferencesManager.notificationsEnabled.first()
        if (!isEnabled) return Result.success()

        val userId = preferencesManager.userId.first() ?: return Result.success()
        val budgetAlerts = preferencesManager.budgetAlerts.first()

        try {
            val budgets = repository.getBudgets(userId).first()
            for (budget in budgets) {
                val bws = repository.getBudgetWithSpending(userId, budget)
                if (bws.isExceeded && budgetAlerts) {
                    sendNotification(
                        title = "Budget Exceeded",
                        body = "${budget.category.displayName} budget exceeded by " +
                                "${String.format("%.0f", bws.spentAmount - budget.limitAmount)}"
                    )
                }
            }

            val health = repository.calculateHealthScore(userId)
            if (health.score < 40) {
                sendNotification(
                    title = "Financial Health Warning",
                    body = "Your financial health score is ${health.score}. Review your spending."
                )
            }

            val leaks = repository.detectMoneyLeaks(userId)
            if (leaks.isNotEmpty()) {
                val leak = leaks.first()
                sendNotification(
                    title = "Money Leak Detected",
                    body = leak.annualCost.let {
                        "You spend ${String.format("%.0f", leak.monthlyCost)}/month on ${leak.category}"
                    }
                )
            }

            return Result.success()
        } catch (e: Exception) {
            return Result.retry()
        }
    }

    private fun sendNotification(title: String, body: String) {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, "paywise_notifications")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(applicationContext)
                .notify(System.currentTimeMillis().toInt(), notification)
        }
    }
}

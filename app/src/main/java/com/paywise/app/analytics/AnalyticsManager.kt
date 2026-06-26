package com.paywise.app.analytics

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.perf.metric.Trace
import com.paywise.app.domain.model.*
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsManager @Inject constructor(
    @ApplicationContext private val context: android.content.Context
) {
    private val analytics = FirebaseAnalytics.getInstance(context)
    private val crashlytics = FirebaseCrashlytics.getInstance()

    fun logExpenseCreated(expense: Expense) {
        val bundle = Bundle().apply {
            putString("category", expense.category.name)
            putDouble("amount", expense.amount)
            putBoolean("is_recurring", expense.isRecurring)
        }
        analytics.logEvent("expense_created", bundle)
    }

    fun logExpenseDeleted() {
        analytics.logEvent("expense_deleted", null)
    }

    fun logGoalCompleted(goal: FinancialGoal) {
        val bundle = Bundle().apply {
            putString("goal_type", goal.type.name)
            putDouble("target_amount", goal.targetAmount)
            putInt("priority", goal.priority)
        }
        analytics.logEvent("goal_completed", bundle)
    }

    fun logGoalCreated(goal: FinancialGoal) {
        val bundle = Bundle().apply {
            putString("goal_type", goal.type.name)
            putDouble("target_amount", goal.targetAmount)
            putInt("priority", goal.priority)
        }
        analytics.logEvent("goal_created", bundle)
    }

    fun logPremiumPurchased(plan: String) {
        analytics.logEvent("premium_purchased", Bundle().apply {
            putString("plan", plan)
        })
    }

    fun logSimulationRun(simulation: Simulation) {
        analytics.logEvent("simulation_run", Bundle().apply {
            putString("type", simulation.type.name)
            putDouble("monthly_cost", simulation.monthlyCost)
            putDouble("one_time_cost", simulation.oneTimeCost)
        })
    }

    fun logBudgetCreated(budget: Budget) {
        analytics.logEvent("budget_created", Bundle().apply {
            putString("category", budget.category.name)
            putDouble("limit", budget.limitAmount)
        })
    }

    fun logBudgetExceeded(budget: Budget, overspentBy: Double) {
        analytics.logEvent("budget_exceeded", Bundle().apply {
            putString("category", budget.category.name)
            putDouble("overspent_by", overspentBy)
        })
    }

    fun logOnboardingCompleted() {
        analytics.logEvent("onboarding_completed", null)
    }

    fun logSignIn(method: String) {
        analytics.logEvent("login", Bundle().apply {
            putString("method", method)
        })
    }

    fun logAppOpen() {
        analytics.logEvent(FirebaseAnalytics.Event.APP_OPEN, null)
    }

    fun logScreenView(screenName: String) {
        analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
        })
    }

    fun logError(error: Throwable) {
        crashlytics.recordException(error)
    }

    fun logError(message: String) {
        crashlytics.log(message)
    }

    fun setUserProperty(userId: String, isPremium: Boolean) {
        analytics.setUserProperty("user_id", userId.take(10))
        analytics.setUserProperty("is_premium", isPremium.toString())
    }

    fun setCrashlyticsUserId(userId: String) {
        crashlytics.setUserId(userId)
    }

    fun startTrace(traceName: String): Trace {
        return FirebasePerformance.getInstance().newTrace(traceName).apply { start() }
    }
}

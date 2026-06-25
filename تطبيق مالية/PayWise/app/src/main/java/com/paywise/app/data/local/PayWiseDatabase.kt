package com.paywise.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.paywise.app.domain.model.*

@Database(
    entities = [
        UserProfile::class,
        Expense::class,
        Budget::class,
        FinancialGoal::class,
        EmergencyFund::class,
        Subscription::class,
        Simulation::class
    ],
    version = 1,
    exportSchema = false
)
abstract class PayWiseDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun budgetDao(): BudgetDao
    abstract fun goalDao(): FinancialGoalDao
    abstract fun emergencyFundDao(): EmergencyFundDao
    abstract fun subscriptionDao(): SubscriptionDao
    abstract fun simulationDao(): SimulationDao

    companion object {
        const val DATABASE_NAME = "paywise_db"
    }
}

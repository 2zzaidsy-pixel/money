package com.paywise.app.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index
import java.util.UUID

// --- ENUMS ---

enum class ExpenseCategory(val displayName: String, val isEssential: Boolean) {
    RENT("Rent", essential = true),
    ELECTRICITY("Electricity", essential = true),
    WATER("Water", essential = true),
    INTERNET("Internet", essential = true),
    LOANS("Loans", essential = true),
    FUEL("Fuel", essential = true),
    GROCERIES("Groceries", essential = true),
    RESTAURANTS("Restaurants", essential = false),
    COFFEE("Coffee", essential = false),
    ENTERTAINMENT("Entertainment", essential = false),
    SHOPPING("Shopping", essential = false),
    SUBSCRIPTIONS("Subscriptions", essential = false),
    MEDICAL("Medical", essential = true),
    CAR_REPAIRS("Car Repairs", essential = false),
    HOME_MAINTENANCE("Home Maintenance", essential = true),
    FAMILY_EMERGENCY("Family Emergency", essential = true)
}

enum class GoalType(val displayName: String) {
    CAR("Car"),
    MARRIAGE("Marriage"),
    TRAVEL("Travel"),
    HOUSE("House"),
    EDUCATION("Education"),
    NEW_PHONE("New Phone"),
    CUSTOM("Custom")
}

enum class FinancialHealthLevel { EXCELLENT, GOOD, WARNING, RISK }

enum class BudgetPeriod { WEEKLY, MONTHLY, YEARLY }

enum class SubscriptionPlan { FREE, PREMIUM }

enum class SimulationType { LOAN, RENT_INCREASE, TRAVEL, CUSTOM }

// --- ROOM ENTITIES ---

@Entity(tableName = "users")
data class UserProfile(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val email: String = "",
    val currency: String = "SAR",
    val salaryAmount: Double = 0.0,
    val salaryDay: Int = 1,
    val country: String = "",
    val preferredLanguage: String = "en",
    val isGuest: Boolean = false,
    val plan: SubscriptionPlan = SubscriptionPlan.FREE,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "expenses",
    foreignKeys = [ForeignKey(
        entity = UserProfile::class,
        parentColumns = ["id"],
        childColumns = ["userId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("userId"), Index("date"), Index("category")]
)
data class Expense(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val amount: Double,
    val category: ExpenseCategory,
    val date: Long = System.currentTimeMillis(),
    val note: String = "",
    val isRecurring: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "budgets",
    foreignKeys = [ForeignKey(
        entity = UserProfile::class,
        parentColumns = ["id"],
        childColumns = ["userId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("userId")]
)
data class Budget(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val category: ExpenseCategory,
    val limitAmount: Double,
    val period: BudgetPeriod = BudgetPeriod.MONTHLY,
    val month: Int,
    val year: Int,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "financial_goals",
    foreignKeys = [ForeignKey(
        entity = UserProfile::class,
        parentColumns = ["id"],
        childColumns = ["userId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("userId")]
)
data class FinancialGoal(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val title: String,
    val type: GoalType,
    val targetAmount: Double,
    val currentAmount: Double = 0.0,
    val deadline: Long,
    val priority: Int = 1,
    val monthlySavingRequired: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "emergency_fund",
    foreignKeys = [ForeignKey(
        entity = UserProfile::class,
        parentColumns = ["id"],
        childColumns = ["userId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("userId")]
)
data class EmergencyFund(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val currentAmount: Double = 0.0,
    val targetMonths: Int = 6,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "subscriptions",
    foreignKeys = [ForeignKey(
        entity = UserProfile::class,
        parentColumns = ["id"],
        childColumns = ["userId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("userId")]
)
data class Subscription(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val name: String,
    val monthlyCost: Double,
    val annualCost: Double,
    val category: String = "",
    val isActive: Boolean = true,
    val nextBillingDate: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "simulations",
    foreignKeys = [ForeignKey(
        entity = UserProfile::class,
        parentColumns = ["id"],
        childColumns = ["userId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("userId")]
)
data class Simulation(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val type: SimulationType,
    val title: String,
    val monthlyCost: Double,
    val oneTimeCost: Double = 0.0,
    val description: String = "",
    val impactScore: Double = 0.0,
    val impactDetails: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

// --- UI MODELS ---

data class SalaryInfo(
    val salary: Double,
    val totalSpent: Double,
    val remaining: Double,
    val percentageUsed: Float
)

data class FinancialHealthScore(
    val score: Int,
    val level: FinancialHealthLevel,
    val savingRatio: Double,
    val budgetCompliance: Double,
    val essentialRatio: Double,
    val unnecessaryRatio: Double,
    val emergencyFundStrength: Double
)

data class SurvivalDateInfo(
    val survivalDate: Long?,
    val remainingBeforeNextSalary: Double?,
    val isRunningOut: Boolean,
    val daysRemaining: Int?
)

data class MonthlyPrediction(
    val comparisonToLastMonth: Double,
    val predictedSaving: Double,
    val isOnTrack: Boolean,
    val message: String
)

data class BudgetWithSpending(
    val budget: Budget,
    val spentAmount: Double,
    val remainingAmount: Double,
    val percentageUsed: Float,
    val isExceeded: Boolean
)

data class GoalProgress(
    val goal: FinancialGoal,
    val percentageComplete: Float,
    val remainingAmount: Double,
    val estimatedCompletionDate: Long?,
    val isOnTrack: Boolean
)

data class MoneyLeak(
    val category: String,
    val monthlyCost: Double,
    val annualCost: Double,
    val suggestion: String
)

data class SimulationResult(
    val newRemainingBalance: Double,
    val savingsReductionPercent: Double,
    val healthScoreImpact: Int,
    val goalDelays: List<GoalDelay>
)

data class GoalDelay(
    val goalTitle: String,
    val additionalMonths: Int
)

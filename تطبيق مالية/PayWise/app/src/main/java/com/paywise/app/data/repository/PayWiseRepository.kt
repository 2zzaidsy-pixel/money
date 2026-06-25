package com.paywise.app.data.repository

import com.paywise.app.domain.model.*
import kotlinx.coroutines.flow.Flow

interface PayWiseRepository {
    // User Profile
    fun getUser(userId: String): Flow<UserProfile?>
    suspend fun getUserOnce(userId: String): UserProfile?
    suspend fun createUser(user: UserProfile)
    suspend fun updateUser(user: UserProfile)
    suspend fun updateSalary(userId: String, salary: Double, salaryDay: Int)
    suspend fun updateLanguage(userId: String, lang: String)
    fun getGuestUser(): UserProfile?

    // Expenses
    fun getExpenses(userId: String): Flow<List<Expense>>
    suspend fun getExpenseById(userId: String, expenseId: String): Expense?
    fun getExpensesByCategory(userId: String, category: ExpenseCategory): Flow<List<Expense>>
    fun getExpensesBetween(userId: String, startDate: Long, endDate: Long): Flow<List<Expense>>
    fun getTotalSpentBetween(userId: String, startDate: Long, endDate: Long): Flow<Double?>
    suspend fun getTotalSpentBetweenOnce(userId: String, startDate: Long, endDate: Long): Double?
    fun getCategoryTotals(userId: String, startDate: Long, endDate: Long): Flow<List<CategoryTotal>>
    fun searchExpenses(userId: String, query: String): Flow<List<Expense>>
    suspend fun addExpense(expense: Expense)
    suspend fun updateExpense(expense: Expense)
    suspend fun deleteExpense(expense: Expense)

    // Budgets
    fun getBudgets(userId: String): Flow<List<Budget>>
    fun getBudgetsForMonth(userId: String, month: Int, year: Int): Flow<List<Budget>>
    fun getBudgetByCategory(userId: String, category: ExpenseCategory, month: Int, year: Int): Flow<Budget?>
    suspend fun getBudgetById(userId: String, budgetId: String): Budget?
    suspend fun addBudget(budget: Budget)
    suspend fun updateBudget(budget: Budget)
    suspend fun deleteBudget(budget: Budget)

    // Goals
    fun getGoals(userId: String): Flow<List<FinancialGoal>>
    suspend fun getGoalById(userId: String, goalId: String): FinancialGoal?
    fun getGoalFlow(userId: String, goalId: String): Flow<FinancialGoal?>
    fun getTotalSaved(userId: String): Flow<Double?>
    fun getTotalTarget(userId: String): Flow<Double?>
    suspend fun addGoal(goal: FinancialGoal)
    suspend fun updateGoal(goal: FinancialGoal)
    suspend fun deleteGoal(goal: FinancialGoal)
    suspend fun updateGoalProgress(goalId: String, amount: Double)

    // Emergency Fund
    fun getEmergencyFund(userId: String): Flow<EmergencyFund?>
    suspend fun getEmergencyFundOnce(userId: String): EmergencyFund?
    suspend fun saveEmergencyFund(fund: EmergencyFund)

    // Subscriptions
    fun getSubscriptions(userId: String): Flow<List<Subscription>>
    fun getActiveSubscriptions(userId: String): Flow<List<Subscription>>
    fun getTotalMonthlySubscriptionCost(userId: String): Flow<Double?>
    suspend fun addSubscription(subscription: Subscription)
    suspend fun updateSubscription(subscription: Subscription)
    suspend fun deleteSubscription(subscription: Subscription)
    suspend fun toggleSubscription(id: String, isActive: Boolean)

    // Simulations
    fun getSimulations(userId: String): Flow<List<Simulation>>
    suspend fun addSimulation(simulation: Simulation)
    suspend fun deleteSimulation(simulation: Simulation)

    // Calculations
    suspend fun calculateSalaryInfo(userId: String): SalaryInfo
    suspend fun calculateHealthScore(userId: String): FinancialHealthScore
    suspend fun calculateSurvivalDate(userId: String): SurvivalDateInfo
    suspend fun calculateMonthlyPrediction(userId: String): MonthlyPrediction
    suspend fun detectMoneyLeaks(userId: String): List<MoneyLeak>
    suspend fun simulateFinancialDecision(
        userId: String,
        monthlyCost: Double,
        oneTimeCost: Double,
        type: SimulationType
    ): SimulationResult
    suspend fun calculateGoalProgress(goal: FinancialGoal): GoalProgress
    suspend fun getBudgetWithSpending(userId: String, budget: Budget): BudgetWithSpending
}

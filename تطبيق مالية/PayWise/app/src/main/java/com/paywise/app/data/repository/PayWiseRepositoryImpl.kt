package com.paywise.app.data.repository

import com.paywise.app.data.local.*
import com.paywise.app.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt
import kotlin.math.min

@Singleton
class PayWiseRepositoryImpl @Inject constructor(
    private val expenseDao: ExpenseDao,
    private val budgetDao: BudgetDao,
    private val goalDao: FinancialGoalDao,
    private val emergencyFundDao: EmergencyFundDao,
    private val subscriptionDao: SubscriptionDao,
    private val simulationDao: SimulationDao,
    private val userProfileDao: UserProfileDao
) : PayWiseRepository {

    // --- USER ---

    override fun getUser(userId: String): Flow<UserProfile?> = userProfileDao.getUser(userId)

    override suspend fun getUserOnce(userId: String): UserProfile? {
        return userProfileDao.getUser(userId).first()
    }

    override suspend fun createUser(user: UserProfile) = userProfileDao.insertUser(user)

    override suspend fun updateUser(user: UserProfile) = userProfileDao.updateUser(user)

    override suspend fun updateSalary(userId: String, salary: Double, salaryDay: Int) =
        userProfileDao.updateSalary(userId, salary, salaryDay)

    override suspend fun updateLanguage(userId: String, lang: String) =
        userProfileDao.updateLanguage(userId, lang)

    override fun getGuestUser(): UserProfile? = null // handled via UserProfileDao

    // --- EXPENSES ---

    override fun getExpenses(userId: String): Flow<List<Expense>> = expenseDao.getExpenses(userId)

    override suspend fun getExpenseById(userId: String, expenseId: String): Expense? =
        expenseDao.getExpenseById(userId, expenseId)

    override fun getExpensesByCategory(userId: String, category: ExpenseCategory): Flow<List<Expense>> =
        expenseDao.getExpensesByCategory(userId, category)

    override fun getExpensesBetween(userId: String, startDate: Long, endDate: Long): Flow<List<Expense>> =
        expenseDao.getExpensesBetween(userId, startDate, endDate)

    override fun getTotalSpentBetween(userId: String, startDate: Long, endDate: Long): Flow<Double?> =
        expenseDao.getTotalSpentBetween(userId, startDate, endDate)

    override suspend fun getTotalSpentBetweenOnce(userId: String, startDate: Long, endDate: Long): Double? =
        expenseDao.getTotalSpentBetweenOnce(userId, startDate, endDate)

    override fun getCategoryTotals(userId: String, startDate: Long, endDate: Long): Flow<List<CategoryTotal>> =
        expenseDao.getCategoryTotals(userId, startDate, endDate)

    override fun searchExpenses(userId: String, query: String): Flow<List<Expense>> =
        expenseDao.searchExpenses(userId, query)

    override suspend fun addExpense(expense: Expense) = expenseDao.insertExpense(expense)

    override suspend fun updateExpense(expense: Expense) = expenseDao.updateExpense(expense)

    override suspend fun deleteExpense(expense: Expense) = expenseDao.deleteExpense(expense)

    // --- BUDGETS ---

    override fun getBudgets(userId: String): Flow<List<Budget>> = budgetDao.getBudgets(userId)

    override fun getBudgetsForMonth(userId: String, month: Int, year: Int): Flow<List<Budget>> =
        budgetDao.getBudgetsForMonth(userId, month, year)

    override fun getBudgetByCategory(userId: String, category: ExpenseCategory, month: Int, year: Int): Flow<Budget?> =
        budgetDao.getBudgetByCategory(userId, category, month, year)

    override suspend fun getBudgetById(userId: String, budgetId: String): Budget? =
        budgetDao.getBudgetById(userId, budgetId)

    override suspend fun addBudget(budget: Budget) = budgetDao.insertBudget(budget)

    override suspend fun updateBudget(budget: Budget) = budgetDao.updateBudget(budget)

    override suspend fun deleteBudget(budget: Budget) = budgetDao.deleteBudget(budget)

    // --- GOALS ---

    override fun getGoals(userId: String): Flow<List<FinancialGoal>> = goalDao.getGoals(userId)

    override suspend fun getGoalById(userId: String, goalId: String): FinancialGoal? =
        goalDao.getGoalById(userId, goalId)

    override fun getGoalFlow(userId: String, goalId: String): Flow<FinancialGoal?> =
        goalDao.getGoalFlow(userId, goalId)

    override fun getTotalSaved(userId: String): Flow<Double?> = goalDao.getTotalSaved(userId)

    override fun getTotalTarget(userId: String): Flow<Double?> = goalDao.getTotalTarget(userId)

    override suspend fun addGoal(goal: FinancialGoal) = goalDao.insertGoal(goal)

    override suspend fun updateGoal(goal: FinancialGoal) = goalDao.updateGoal(goal)

    override suspend fun deleteGoal(goal: FinancialGoal) = goalDao.deleteGoal(goal)

    override suspend fun updateGoalProgress(goalId: String, amount: Double) =
        goalDao.updateGoalProgress(goalId, amount)

    // --- EMERGENCY FUND ---

    override fun getEmergencyFund(userId: String): Flow<EmergencyFund?> =
        emergencyFundDao.getFund(userId)

    override suspend fun getEmergencyFundOnce(userId: String): EmergencyFund? =
        emergencyFundDao.getFundOnce(userId)

    override suspend fun saveEmergencyFund(fund: EmergencyFund) =
        emergencyFundDao.insertOrUpdate(fund)

    // --- SUBSCRIPTIONS ---

    override fun getSubscriptions(userId: String): Flow<List<Subscription>> =
        subscriptionDao.getSubscriptions(userId)

    override fun getActiveSubscriptions(userId: String): Flow<List<Subscription>> =
        subscriptionDao.getActiveSubscriptions(userId)

    override fun getTotalMonthlySubscriptionCost(userId: String): Flow<Double?> =
        subscriptionDao.getTotalMonthlyCost(userId)

    override suspend fun addSubscription(subscription: Subscription) =
        subscriptionDao.insertSubscription(subscription)

    override suspend fun updateSubscription(subscription: Subscription) =
        subscriptionDao.updateSubscription(subscription)

    override suspend fun deleteSubscription(subscription: Subscription) =
        subscriptionDao.deleteSubscription(subscription)

    override suspend fun toggleSubscription(id: String, isActive: Boolean) =
        subscriptionDao.toggleSubscription(id, isActive)

    // --- SIMULATIONS ---

    override fun getSimulations(userId: String): Flow<List<Simulation>> =
        simulationDao.getSimulations(userId)

    override suspend fun addSimulation(simulation: Simulation) =
        simulationDao.insertSimulation(simulation)

    override suspend fun deleteSimulation(simulation: Simulation) =
        simulationDao.deleteSimulation(simulation)

    // --- CALCULATIONS ---

    override suspend fun calculateSalaryInfo(userId: String): SalaryInfo {
        val user = getUserOnce(userId) ?: return SalaryInfo(0.0, 0.0, 0.0, 0f)
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)
        val startOfMonth = getStartOfMonth(currentMonth, currentYear)
        val endOfMonth = getEndOfMonth(currentMonth, currentYear)
        val totalSpent = getTotalSpentBetweenOnce(userId, startOfMonth, endOfMonth) ?: 0.0
        val remaining = user.salaryAmount - totalSpent
        val percentage = if (user.salaryAmount > 0) min((totalSpent / user.salaryAmount).toFloat(), 1f) else 0f
        return SalaryInfo(user.salaryAmount, totalSpent, remaining, percentage)
    }

    override suspend fun calculateHealthScore(userId: String): FinancialHealthScore {
        val user = getUserOnce(userId) ?: return FinancialHealthScore(0, FinancialHealthLevel.RISK, 0.0, 0.0, 0.0, 0.0, 0.0)
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)
        val startOfMonth = getStartOfMonth(currentMonth, currentYear)
        val endOfMonth = getEndOfMonth(currentMonth, currentYear)

        val totalSpent = getTotalSpentBetweenOnce(userId, startOfMonth, endOfMonth) ?: 0.0
        val categories = expenseDao.getCategoryTotals(userId, startOfMonth, endOfMonth).first()

        val essentialSpent = categories.filter { it.category.isEssential }.sumOf { it.total }
        val unnecessarySpent = categories.filter { !it.category.isEssential }.sumOf { it.total }

        val savingRatio = if (user.salaryAmount > 0) (user.salaryAmount - totalSpent) / user.salaryAmount else 0.0
        val essentialRatio = if (totalSpent > 0) essentialSpent / totalSpent else 0.0
        val unnecessaryRatio = if (totalSpent > 0) unnecessarySpent / totalSpent else 0.0

        // Budget compliance
        val budgets = budgetDao.getBudgetsForMonth(userId, currentMonth, currentYear).first()
        val budgetCompliance = if (budgets.isNotEmpty()) {
            val compliant = budgets.count { budget ->
                val spent = categories.find { it.category == budget.category }?.total ?: 0.0
                spent <= budget.limitAmount
            }
            compliant.toDouble() / budgets.size
        } else 0.5

        // Emergency fund strength
        val emergencyFund = emergencyFundDao.getFundOnce(userId)
        val monthlyExpenses = totalSpent
        val emergencyStrength = if (monthlyExpenses > 0 && emergencyFund != null) {
            min(emergencyFund.currentAmount / (monthlyExpenses * 6), 1.0)
        } else 0.0

        // Calculate score (0-100)
        var score = 0
        score += (savingRatio * 25).roundToInt().coerceIn(0, 25)
        score += (budgetCompliance * 25).roundToInt().coerceIn(0, 25)
        score += ((1 - essentialRatio.coerceIn(0.3, 0.7)) * 20).roundToInt().coerceIn(0, 20)
        score += ((1 - unnecessaryRatio) * 15).roundToInt().coerceIn(0, 15)
        score += (emergencyStrength * 15).roundToInt().coerceIn(0, 15)

        val level = when {
            score >= 80 -> FinancialHealthLevel.EXCELLENT
            score >= 60 -> FinancialHealthLevel.GOOD
            score >= 40 -> FinancialHealthLevel.WARNING
            else -> FinancialHealthLevel.RISK
        }

        return FinancialHealthScore(score, level, savingRatio, budgetCompliance, essentialRatio, unnecessaryRatio, emergencyStrength)
    }

    override suspend fun calculateSurvivalDate(userId: String): SurvivalDateInfo {
        val user = getUserOnce(userId) ?: return SurvivalDateInfo(null, null, false, null)
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)
        val startOfMonth = getStartOfMonth(currentMonth, currentYear)
        val endOfMonth = getEndOfMonth(currentMonth, currentYear)

        val daysPassed = calendar.get(Calendar.DAY_OF_MONTH)
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val totalSpent = getTotalSpentBetweenOnce(userId, startOfMonth, endOfMonth) ?: 0.0

        if (daysPassed == 0 || totalSpent == 0.0) {
            return SurvivalDateInfo(null, user.salaryAmount, false, null)
        }

        val dailySpendRate = totalSpent / daysPassed
        val remainingBalance = user.salaryAmount - totalSpent

        if (dailySpendRate <= 0) {
            return SurvivalDateInfo(null, remainingBalance, false, null)
        }

        val daysRemaining = (remainingBalance / dailySpendRate).roundToInt()
        val survivalCal = calendar.clone() as Calendar
        survivalCal.add(Calendar.DAY_OF_MONTH, daysRemaining)

        val nextSalaryDay = getNextSalaryDay(user.salaryDay)

        if (remainingBalance < 0) {
            return SurvivalDateInfo(null, 0.0, true, 0)
        }

        if (survivalCal.after(nextSalaryDay)) {
            val remainingAfterSalary = remainingBalance - dailySpendRate * daysBetween(calendar, nextSalaryDay)
            return SurvivalDateInfo(null, remainingAfterSalary.coerceAtLeast(0.0), false, daysBetween(calendar, nextSalaryDay))
        }

        return SurvivalDateInfo(survivalCal.timeInMillis, null, false, daysRemaining)
    }

    override suspend fun calculateMonthlyPrediction(userId: String): MonthlyPrediction {
        val user = getUserOnce(userId) ?: return MonthlyPrediction(0.0, 0.0, false, "")
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)
        val prevMonth = if (currentMonth == 0) 11 else currentMonth - 1
        val prevYear = if (currentMonth == 0) currentYear - 1 else currentYear

        val startOfMonth = getStartOfMonth(currentMonth, currentYear)
        val endOfMonth = getEndOfMonth(currentMonth, currentYear)
        val startOfPrevMonth = getStartOfMonth(prevMonth, prevYear)
        val endOfPrevMonth = getEndOfMonth(prevMonth, prevYear)

        val currentSpent = getTotalSpentBetweenOnce(userId, startOfMonth, endOfMonth) ?: 0.0
        val prevSpent = getTotalSpentBetweenOnce(userId, startOfPrevMonth, endOfPrevMonth) ?: 0.0

        val comparison = if (prevSpent > 0) ((currentSpent - prevSpent) / prevSpent) * 100 else 0.0
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val daysPassed = calendar.get(Calendar.DAY_OF_MONTH)
        val projectedSpending = if (daysPassed > 0) (currentSpent / daysPassed) * daysInMonth else 0.0
        val predictedSaving = (user.salaryAmount - projectedSpending).coerceAtLeast(0.0)
        val isOnTrack = predictedSaving > 0 && comparison <= 10

        val message = when {
            comparison > 20 -> "You are spending ${comparison.roundToInt()}% more than last month."
            comparison < -10 -> "Great! You are spending ${(-comparison).roundToInt()}% less than last month."
            predictedSaving > 0 -> "You will save approximately ${predictedSaving.roundToInt()} $${user.currency} this month."
            else -> "Warning: You may run out of money before month-end."
        }

        return MonthlyPrediction(comparison, predictedSaving, isOnTrack, message)
    }

    override suspend fun detectMoneyLeaks(userId: String): List<MoneyLeak> {
        val user = getUserOnce(userId) ?: return emptyList()
        val calendar = Calendar.getInstance()
        val startOfMonth = getStartOfMonth(calendar.get(Calendar.MONTH), calendar.get(Calendar.YEAR))
        val endOfMonth = getEndOfMonth(calendar.get(Calendar.MONTH), calendar.get(Calendar.YEAR))

        val leaks = mutableListOf<MoneyLeak>()
        val categories = expenseDao.getCategoryTotals(userId, startOfMonth, endOfMonth).first()

        // Coffee leak detection
        val coffeeSpent = categories.find { it.category == ExpenseCategory.COFFEE }?.total ?: 0.0
        if (coffeeSpent > 50) {
            leaks.add(MoneyLeak(
                category = "Coffee",
                monthlyCost = coffeeSpent,
                annualCost = coffeeSpent * 12,
                suggestion = "Consider reducing coffee expenses. Try making coffee at home!"
            ))
        }

        // Restaurants leak detection
        val restaurantSpent = categories.find { it.category == ExpenseCategory.RESTAURANTS }?.total ?: 0.0
        if (restaurantSpent > 200) {
            leaks.add(MoneyLeak(
                category = "Restaurants",
                monthlyCost = restaurantSpent,
                annualCost = restaurantSpent * 12,
                suggestion = "Eating out is costing you ${restaurantSpent.roundToInt()} $${user.currency}/month. Try meal prepping!"
            ))
        }

        // Subscriptions
        val subscriptionSpent = categories.find { it.category == ExpenseCategory.SUBSCRIPTIONS }?.total ?: 0.0
        if (subscriptionSpent > 30) {
            leaks.add(MoneyLeak(
                category = "Subscriptions",
                monthlyCost = subscriptionSpent,
                annualCost = subscriptionSpent * 12,
                suggestion = "You have ${subscriptionSpent.roundToInt()} $${user.currency} in subscriptions. Review unused ones."
            ))
        }

        return leaks
    }

    override suspend fun simulateFinancialDecision(
        userId: String,
        monthlyCost: Double,
        oneTimeCost: Double,
        type: SimulationType
    ): SimulationResult {
        val user = getUserOnce(userId) ?: return SimulationResult(0.0, 0.0, 0, emptyList())
        val salaryInfo = calculateSalaryInfo(userId)
        val healthScore = calculateHealthScore(userId)
        val goals = goalDao.getGoals(userId).first()

        val newRemaining = salaryInfo.remaining - monthlyCost - oneTimeCost
        val savingsReduction = if (salaryInfo.salary > 0) ((monthlyCost + oneTimeCost) / salaryInfo.salary) * 100 else 0.0
        val healthScoreImpact = (savingsReduction / 2).roundToInt()

        val goalDelays = goals.mapNotNull { goal ->
            if (goal.monthlySavingRequired > 0 && monthlyCost > 0) {
                val currentMonthlySaving = goal.monthlySavingRequired
                val remainingGoal = goal.targetAmount - goal.currentAmount
                val originalMonths = (remainingGoal / currentMonthlySaving).roundToInt()
                val reducedSaving = (currentMonthlySaving - monthlyCost * 0.1).coerceAtLeast(0.0)
                val newMonths = if (reducedSaving > 0) (remainingGoal / reducedSaving).roundToInt() else Int.MAX_VALUE
                val additionalMonths = (newMonths - originalMonths).coerceAtLeast(0)
                if (additionalMonths > 0) GoalDelay(goal.title, additionalMonths) else null
            } else null
        }

        return SimulationResult(
            newRemainingBalance = newRemaining,
            savingsReductionPercent = savingsReduction,
            healthScoreImpact = healthScoreImpact,
            goalDelays = goalDelays
        )
    }

    override suspend fun calculateGoalProgress(goal: FinancialGoal): GoalProgress {
        val percentage = if (goal.targetAmount > 0) (goal.currentAmount / goal.targetAmount).toFloat().coerceIn(0f, 1f) else 0f
        val remainingAmount = (goal.targetAmount - goal.currentAmount).coerceAtLeast(0.0)
        val now = System.currentTimeMillis()
        val isOnTrack = goal.monthlySavingRequired > 0 && (goal.currentAmount / goal.monthlySavingRequired) <= monthsBetween(now, goal.deadline)
        return GoalProgress(goal, percentage, remainingAmount, goal.deadline, isOnTrack)
    }

    override suspend fun getBudgetWithSpending(userId: String, budget: Budget): BudgetWithSpending {
        val startOfMonth = getStartOfMonth(budget.month, budget.year)
        val endOfMonth = getEndOfMonth(budget.month, budget.year)
        val categories = expenseDao.getCategoryTotals(userId, startOfMonth, endOfMonth).first()
        val spent = categories.find { it.category == budget.category }?.total ?: 0.0
        val remaining = (budget.limitAmount - spent).coerceAtLeast(0.0)
        val percentage = if (budget.limitAmount > 0) (spent / budget.limitAmount).toFloat().coerceIn(0f, 1.5f) else 0f
        return BudgetWithSpending(budget, spent, remaining, percentage, spent > budget.limitAmount)
    }

    // --- HELPERS ---

    private fun getStartOfMonth(month: Int, year: Int): Long {
        val cal = Calendar.getInstance()
        cal.set(year, month, 1, 0, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun getEndOfMonth(month: Int, year: Int): Long {
        val cal = Calendar.getInstance()
        cal.set(year, month, 1, 0, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        return cal.timeInMillis
    }

    private fun getNextSalaryDay(salaryDay: Int): Calendar {
        val cal = Calendar.getInstance()
        val today = cal.get(Calendar.DAY_OF_MONTH)
        if (today < salaryDay) {
            cal.set(Calendar.DAY_OF_MONTH, salaryDay)
        } else {
            cal.add(Calendar.MONTH, 1)
            cal.set(Calendar.DAY_OF_MONTH, min(salaryDay, cal.getActualMaximum(Calendar.DAY_OF_MONTH)))
        }
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        return cal
    }

    private fun daysBetween(from: Calendar, to: Calendar): Int {
        val diff = to.timeInMillis - from.timeInMillis
        return (diff / (1000 * 60 * 60 * 24)).toInt().coerceAtLeast(0)
    }

    private fun monthsBetween(from: Long, to: Long): Int {
        val fromCal = Calendar.getInstance().apply { timeInMillis = from }
        val toCal = Calendar.getInstance().apply { timeInMillis = to }
        return ((toCal.get(Calendar.YEAR) - fromCal.get(Calendar.YEAR)) * 12 +
                (toCal.get(Calendar.MONTH) - fromCal.get(Calendar.MONTH))).coerceAtLeast(0)
    }
}

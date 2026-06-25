package com.paywise.app.data.local

import androidx.room.*
import com.paywise.app.domain.model.Budget
import com.paywise.app.domain.model.ExpenseCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets WHERE userId = :userId ORDER BY year DESC, month DESC")
    fun getBudgets(userId: String): Flow<List<Budget>>

    @Query("SELECT * FROM budgets WHERE userId = :userId AND month = :month AND year = :year")
    fun getBudgetsForMonth(userId: String, month: Int, year: Int): Flow<List<Budget>>

    @Query("SELECT * FROM budgets WHERE userId = :userId AND category = :category AND month = :month AND year = :year")
    fun getBudgetByCategory(userId: String, category: ExpenseCategory, month: Int, year: Int): Flow<Budget?>

    @Query("SELECT * FROM budgets WHERE userId = :userId AND id = :budgetId")
    suspend fun getBudgetById(userId: String, budgetId: String): Budget?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: Budget)

    @Update
    suspend fun updateBudget(budget: Budget)

    @Delete
    suspend fun deleteBudget(budget: Budget)

    @Query("DELETE FROM budgets WHERE userId = :userId AND id = :budgetId")
    suspend fun deleteBudgetById(userId: String, budgetId: String)
}

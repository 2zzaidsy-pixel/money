package com.paywise.app.data.local

import androidx.room.*
import com.paywise.app.domain.model.Expense
import com.paywise.app.domain.model.ExpenseCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses WHERE userId = :userId ORDER BY date DESC")
    fun getExpenses(userId: String): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE userId = :userId AND id = :expenseId")
    suspend fun getExpenseById(userId: String, expenseId: String): Expense?

    @Query("SELECT * FROM expenses WHERE userId = :userId AND category = :category ORDER BY date DESC")
    fun getExpensesByCategory(userId: String, category: ExpenseCategory): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE userId = :userId AND date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getExpensesBetween(userId: String, startDate: Long, endDate: Long): Flow<List<Expense>>

    @Query("SELECT SUM(amount) FROM expenses WHERE userId = :userId AND date BETWEEN :startDate AND :endDate")
    fun getTotalSpentBetween(userId: String, startDate: Long, endDate: Long): Flow<Double?>

    @Query("SELECT SUM(amount) FROM expenses WHERE userId = :userId AND date BETWEEN :startDate AND :endDate")
    suspend fun getTotalSpentBetweenOnce(userId: String, startDate: Long, endDate: Long): Double?

    @Query("SELECT category, SUM(amount) as total FROM expenses WHERE userId = :userId AND date BETWEEN :startDate AND :endDate GROUP BY category")
    fun getCategoryTotals(userId: String, startDate: Long, endDate: Long): Flow<List<CategoryTotal>>

    @Query("SELECT * FROM expenses WHERE userId = :userId AND note LIKE '%' || :query || '%' ORDER BY date DESC")
    fun searchExpenses(userId: String, query: String): Flow<List<Expense>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense)

    @Update
    suspend fun updateExpense(expense: Expense)

    @Delete
    suspend fun deleteExpense(expense: Expense)

    @Query("DELETE FROM expenses WHERE userId = :userId AND id = :expenseId")
    suspend fun deleteExpenseById(userId: String, expenseId: String)

    @Query("SELECT COUNT(*) FROM expenses WHERE userId = :userId")
    fun getExpenseCount(userId: String): Flow<Int>
}

data class CategoryTotal(
    val category: ExpenseCategory,
    val total: Double
)

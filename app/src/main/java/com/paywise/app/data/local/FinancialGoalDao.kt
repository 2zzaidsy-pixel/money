package com.paywise.app.data.local

import androidx.room.*
import com.paywise.app.domain.model.FinancialGoal
import kotlinx.coroutines.flow.Flow

@Dao
interface FinancialGoalDao {
    @Query("SELECT * FROM financial_goals WHERE userId = :userId ORDER BY priority ASC")
    fun getGoals(userId: String): Flow<List<FinancialGoal>>

    @Query("SELECT * FROM financial_goals WHERE userId = :userId AND id = :goalId")
    suspend fun getGoalById(userId: String, goalId: String): FinancialGoal?

    @Query("SELECT * FROM financial_goals WHERE userId = :userId AND id = :goalId")
    fun getGoalFlow(userId: String, goalId: String): Flow<FinancialGoal?>

    @Query("SELECT SUM(currentAmount) FROM financial_goals WHERE userId = :userId")
    fun getTotalSaved(userId: String): Flow<Double?>

    @Query("SELECT SUM(targetAmount) FROM financial_goals WHERE userId = :userId")
    fun getTotalTarget(userId: String): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: FinancialGoal)

    @Update
    suspend fun updateGoal(goal: FinancialGoal)

    @Delete
    suspend fun deleteGoal(goal: FinancialGoal)

    @Query("DELETE FROM financial_goals WHERE userId = :userId AND id = :goalId")
    suspend fun deleteGoalById(userId: String, goalId: String)

    @Query("UPDATE financial_goals SET currentAmount = :amount WHERE id = :goalId")
    suspend fun updateGoalProgress(goalId: String, amount: Double)
}

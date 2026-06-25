package com.paywise.app.data.local

import androidx.room.*
import com.paywise.app.domain.model.EmergencyFund
import kotlinx.coroutines.flow.Flow

@Dao
interface EmergencyFundDao {
    @Query("SELECT * FROM emergency_fund WHERE userId = :userId")
    fun getFund(userId: String): Flow<EmergencyFund?>

    @Query("SELECT * FROM emergency_fund WHERE userId = :userId")
    suspend fun getFundOnce(userId: String): EmergencyFund?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(fund: EmergencyFund)

    @Query("UPDATE emergency_fund SET currentAmount = :amount, updatedAt = :updatedAt WHERE userId = :userId")
    suspend fun updateAmount(userId: String, amount: Double, updatedAt: Long = System.currentTimeMillis())
}

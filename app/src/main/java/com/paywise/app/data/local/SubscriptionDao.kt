package com.paywise.app.data.local

import androidx.room.*
import com.paywise.app.domain.model.Subscription
import kotlinx.coroutines.flow.Flow

@Dao
interface SubscriptionDao {
    @Query("SELECT * FROM subscriptions WHERE userId = :userId ORDER BY monthlyCost DESC")
    fun getSubscriptions(userId: String): Flow<List<Subscription>>

    @Query("SELECT * FROM subscriptions WHERE userId = :userId AND isActive = 1")
    fun getActiveSubscriptions(userId: String): Flow<List<Subscription>>

    @Query("SELECT SUM(monthlyCost) FROM subscriptions WHERE userId = :userId AND isActive = 1")
    fun getTotalMonthlyCost(userId: String): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubscription(subscription: Subscription)

    @Update
    suspend fun updateSubscription(subscription: Subscription)

    @Delete
    suspend fun deleteSubscription(subscription: Subscription)

    @Query("UPDATE subscriptions SET isActive = :isActive WHERE id = :id")
    suspend fun toggleSubscription(id: String, isActive: Boolean)
}

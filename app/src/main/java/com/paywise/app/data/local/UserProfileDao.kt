package com.paywise.app.data.local

import androidx.room.*
import com.paywise.app.domain.model.SubscriptionPlan
import com.paywise.app.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM users WHERE id = :userId")
    fun getUser(userId: String): Flow<UserProfile?>

    @Query("SELECT * FROM users WHERE email = :email")
    suspend fun getUserByEmail(email: String): UserProfile?

    @Query("SELECT * FROM users WHERE isGuest = 1")
    suspend fun getGuestUser(): UserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserProfile)

    @Update
    suspend fun updateUser(user: UserProfile)

    @Delete
    suspend fun deleteUser(user: UserProfile)

    @Query("UPDATE users SET salaryAmount = :salary, salaryDay = :salaryDay WHERE id = :userId")
    suspend fun updateSalary(userId: String, salary: Double, salaryDay: Int)

    @Query("UPDATE users SET preferredLanguage = :lang WHERE id = :userId")
    suspend fun updateLanguage(userId: String, lang: String)

    @Query("UPDATE users SET plan = :plan WHERE id = :userId")
    suspend fun updatePlan(userId: String, plan: SubscriptionPlan)

    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<UserProfile>>
}

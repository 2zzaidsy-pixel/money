package com.paywise.app.data.local

import androidx.room.*
import com.paywise.app.domain.model.Simulation
import kotlinx.coroutines.flow.Flow

@Dao
interface SimulationDao {
    @Query("SELECT * FROM simulations WHERE userId = :userId ORDER BY createdAt DESC")
    fun getSimulations(userId: String): Flow<List<Simulation>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSimulation(simulation: Simulation)

    @Delete
    suspend fun deleteSimulation(simulation: Simulation)
}

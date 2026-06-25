package com.paywise.app.di

import android.content.Context
import androidx.room.Room
import com.paywise.app.data.local.*
import com.paywise.app.data.repository.PayWiseRepository
import com.paywise.app.data.repository.PayWiseRepositoryImpl
import com.paywise.app.firebase.FirebaseService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PayWiseDatabase {
        return Room.databaseBuilder(
            context,
            PayWiseDatabase::class.java,
            PayWiseDatabase.DATABASE_NAME
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    @Singleton
    fun provideUserProfileDao(db: PayWiseDatabase): UserProfileDao = db.userProfileDao()

    @Provides
    @Singleton
    fun provideExpenseDao(db: PayWiseDatabase): ExpenseDao = db.expenseDao()

    @Provides
    @Singleton
    fun provideBudgetDao(db: PayWiseDatabase): BudgetDao = db.budgetDao()

    @Provides
    @Singleton
    fun provideGoalDao(db: PayWiseDatabase): FinancialGoalDao = db.goalDao()

    @Provides
    @Singleton
    fun provideEmergencyFundDao(db: PayWiseDatabase): EmergencyFundDao = db.emergencyFundDao()

    @Provides
    @Singleton
    fun provideSubscriptionDao(db: PayWiseDatabase): SubscriptionDao = db.subscriptionDao()

    @Provides
    @Singleton
    fun provideSimulationDao(db: PayWiseDatabase): SimulationDao = db.simulationDao()

    @Provides
    @Singleton
    fun providePreferencesManager(@ApplicationContext context: Context): PreferencesManager {
        return PreferencesManager(context)
    }

    @Provides
    @Singleton
    fun provideFirebaseService(): FirebaseService = FirebaseService()

    @Provides
    @Singleton
    fun provideRepository(
        expenseDao: ExpenseDao,
        budgetDao: BudgetDao,
        goalDao: FinancialGoalDao,
        emergencyFundDao: EmergencyFundDao,
        subscriptionDao: SubscriptionDao,
        simulationDao: SimulationDao,
        userProfileDao: UserProfileDao
    ): PayWiseRepository {
        return PayWiseRepositoryImpl(
            expenseDao, budgetDao, goalDao, emergencyFundDao,
            subscriptionDao, simulationDao, userProfileDao
        )
    }
}

package com.paywise.app.firebase

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.paywise.app.domain.model.*
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseService @Inject constructor() {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    // --- AUTH ---

    suspend fun signInWithEmail(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            result.user?.let { Result.success(it) } ?: Result.failure(Exception("User not found"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signUpWithEmail(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            result.user?.let { Result.success(it) } ?: Result.failure(Exception("User not found"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signInWithGoogle(idToken: String): Result<FirebaseUser> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            result.user?.let { Result.success(it) } ?: Result.failure(Exception("User not found"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createGuestUser(): Result<FirebaseUser> {
        return try {
            val result = auth.signInAnonymously().await()
            result.user?.let { Result.success(it) } ?: Result.failure(Exception("User not found"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    fun signOut() {
        auth.signOut()
    }

    fun isUserLoggedIn(): Boolean = auth.currentUser != null

    // --- FIRESTORE SYNC ---

    suspend fun syncUserProfile(user: UserProfile) {
        try {
            firestore.collection("users").document(user.id).set(user).await()
        } catch (e: Exception) {
            Log.e("FirebaseService", "Error syncing user", e)
        }
    }

    suspend fun syncExpense(expense: Expense) {
        try {
            firestore.collection("users").document(expense.userId)
                .collection("expenses").document(expense.id).set(expense).await()
        } catch (e: Exception) {
            Log.e("FirebaseService", "Error syncing expense", e)
        }
    }

    suspend fun deleteExpenseFromCloud(userId: String, expenseId: String) {
        try {
            firestore.collection("users").document(userId)
                .collection("expenses").document(expenseId).delete().await()
        } catch (e: Exception) {
            Log.e("FirebaseService", "Error deleting expense", e)
        }
    }

    suspend fun syncBudget(budget: Budget) {
        try {
            firestore.collection("users").document(budget.userId)
                .collection("budgets").document(budget.id).set(budget).await()
        } catch (e: Exception) {
            Log.e("FirebaseService", "Error syncing budget", e)
        }
    }

    suspend fun syncGoal(goal: FinancialGoal) {
        try {
            firestore.collection("users").document(goal.userId)
                .collection("goals").document(goal.id).set(goal).await()
        } catch (e: Exception) {
            Log.e("FirebaseService", "Error syncing goal", e)
        }
    }

    suspend fun syncEmergencyFund(userId: String, fund: EmergencyFund) {
        try {
            firestore.collection("users").document(userId)
                .collection("emergency_fund").document(fund.id).set(fund).await()
        } catch (e: Exception) {
            Log.e("FirebaseService", "Error syncing emergency fund", e)
        }
    }

    // --- FCM ---

    fun getFCMToken(onToken: (String) -> Unit) {
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            onToken(token)
        }
    }

    fun subscribeToTopic(topic: String) {
        FirebaseMessaging.getInstance().subscribeToTopic(topic)
    }

    fun unsubscribeFromTopic(topic: String) {
        FirebaseMessaging.getInstance().unsubscribeFromTopic(topic)
    }

    fun sendNotificationToUser(userId: String, title: String, body: String) {
        firestore.collection("users").document(userId)
            .collection("notifications").add(mapOf(
                "title" to title,
                "body" to body,
                "timestamp" to System.currentTimeMillis(),
                "read" to false
            ))
    }
}

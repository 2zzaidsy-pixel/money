package com.paywise.app.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import com.paywise.app.domain.model.*
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurityManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val gson = Gson()

    private val encryptedPrefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            "paywise_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun encryptUserId(userId: String) {
        encryptedPrefs.edit().putString("encrypted_user_id", userId).apply()
    }

    fun decryptUserId(): String? {
        return encryptedPrefs.getString("encrypted_user_id", null)
    }

    fun storeSalaryInfo(salary: Double, currency: String) {
        encryptedPrefs.edit()
            .putString("salary_data", gson.toJson(SalaryData(salary, currency)))
            .apply()
    }

    fun getSalaryInfo(): SalaryData? {
        val json = encryptedPrefs.getString("salary_data", null) ?: return null
        return try { gson.fromJson(json, SalaryData::class.java) } catch (e: Exception) { null }
    }

    fun storeAuthToken(token: String) {
        encryptedPrefs.edit().putString("auth_token", token).apply()
    }

    fun getAuthToken(): String? {
        return encryptedPrefs.getString("auth_token", null)
    }

    fun clearAllSecureData() {
        encryptedPrefs.edit().clear().apply()
    }

    data class SalaryData(val salary: Double, val currency: String)

    fun validateInput(value: String, type: InputType): Boolean {
        return when (type) {
            InputType.AMOUNT -> value.toDoubleOrNull() != null && (value.toDoubleOrNull() ?: 0.0) >= 0
            InputType.PERCENTAGE -> value.toIntOrNull() != null && (value.toIntOrNull() ?: 0) in 0..100
            InputType.DAY_OF_MONTH -> value.toIntOrNull() != null && (value.toIntOrNull() ?: 0) in 1..31
            InputType.PASSWORD -> value.length >= 6
            InputType.EMAIL -> value.contains("@") && value.contains(".")
            InputType.NON_EMPTY -> value.isNotBlank()
        }
    }

    fun sanitizeAmount(value: String): Double {
        return value.toDoubleOrNull()?.let { kotlin.math.abs(it) } ?: 0.0
    }
}

enum class InputType {
    AMOUNT, PERCENTAGE, DAY_OF_MONTH, PASSWORD, EMAIL, NON_EMPTY
}

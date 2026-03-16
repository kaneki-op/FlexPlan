package com.example.flexplan.utils

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class PrefsManager(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_flexplan_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveUserEmail(email: String) {
        sharedPreferences.edit().putString("LoggedInUserEmail", email).apply()
    }

    fun getUserEmail(): String? {
        return sharedPreferences.getString("LoggedInUserEmail", null)
    }

    fun clearSession() {
        sharedPreferences.edit().remove("LoggedInUserEmail").apply()
    }
}

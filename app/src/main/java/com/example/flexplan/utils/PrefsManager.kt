package com.example.flexplan.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class PrefsManager(context: Context) {
    private var sharedPreferences: SharedPreferences? = null

    init {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            sharedPreferences = EncryptedSharedPreferences.create(
                context,
                "secure_flexplan_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.e("PrefsManager", "Error initializing EncryptedSharedPreferences, falling back to standard", e)
            // Fallback to standard SharedPreferences if Encryption fails (common on some budget devices)
            sharedPreferences = context.getSharedPreferences("flexplan_prefs_fallback", Context.MODE_PRIVATE)
        }
    }

    fun saveUserEmail(email: String) {
        sharedPreferences?.edit()?.putString("LoggedInUserEmail", email)?.apply()
    }

    fun getUserEmail(): String? {
        return sharedPreferences?.getString("LoggedInUserEmail", null)
    }

    fun clearSession() {
        sharedPreferences?.edit()?.remove("LoggedInUserEmail")?.apply()
    }
}

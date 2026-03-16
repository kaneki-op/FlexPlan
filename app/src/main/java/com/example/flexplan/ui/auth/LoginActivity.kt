package com.example.flexplan.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.flexplan.R
import com.example.flexplan.data.DatabaseHelper
import com.example.flexplan.ui.home.HomeActivity
import com.example.flexplan.utils.PrefsManager
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class LoginActivity : AppCompatActivity() {

    private lateinit var db: DatabaseHelper
    private lateinit var prefsManager: PrefsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        db = DatabaseHelper(this)
        prefsManager = PrefsManager(this)

        val tilEmail = findViewById<TextInputLayout>(R.id.tilEmail)
        val tilPassword = findViewById<TextInputLayout>(R.id.tilPassword)
        val etEmail = findViewById<TextInputEditText>(R.id.etEmail)
        val etPassword = findViewById<TextInputEditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnRegisterRedirect = findViewById<Button>(R.id.btnRegisterRedirect)
        val tvGuest = findViewById<TextView>(R.id.tvGuest)

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            // Reset errors
            tilEmail.error = null
            tilPassword.error = null

            var isValid = true

            if (email.isEmpty()) {
                tilEmail.error = "Email is required"
                isValid = false
            } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                tilEmail.error = "Please enter a valid email address"
                isValid = false
            }

            if (password.isEmpty()) {
                tilPassword.error = "Password is required"
                isValid = false
            }

            if (!isValid) return@setOnClickListener

            try {
                if (db.checkUser(email, password)) {
                    // Save session for Auto-Login using EncryptedSharedPreferences
                    prefsManager.saveUserEmail(email)

                    Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, HomeActivity::class.java)
                    intent.putExtra("USER_EMAIL", email)
                    startActivity(intent)
                    finish()
                } else {
                    tilPassword.error = "Invalid email or password"
                    Toast.makeText(this, "Invalid credentials", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Database error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        btnRegisterRedirect.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        tvGuest.setOnClickListener {
            Toast.makeText(this, "Continuing as Guest", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, HomeActivity::class.java)
            intent.putExtra("USER_EMAIL", "guest@flexplan.com")
            startActivity(intent)
            finish()
        }
    }
}

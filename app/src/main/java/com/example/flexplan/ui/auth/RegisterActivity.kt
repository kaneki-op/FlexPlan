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
import com.example.flexplan.model.User
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class RegisterActivity : AppCompatActivity() {

    private lateinit var db: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        db = DatabaseHelper(this)

        val tilName = findViewById<TextInputLayout>(R.id.tilName)
        val tilEmail = findViewById<TextInputLayout>(R.id.tilEmail)
        val tilPassword = findViewById<TextInputLayout>(R.id.tilPassword)
        val tilConfirmPassword = findViewById<TextInputLayout>(R.id.tilConfirmPassword)

        val etName = findViewById<TextInputEditText>(R.id.etName)
        val etEmail = findViewById<TextInputEditText>(R.id.etEmail)
        val etPassword = findViewById<TextInputEditText>(R.id.etPassword)
        val etConfirmPassword = findViewById<TextInputEditText>(R.id.etConfirmPassword)
        val btnRegister = findViewById<Button>(R.id.btnRegister)
        val tvLoginRedirect = findViewById<TextView>(R.id.tvLoginRedirect)

        btnRegister.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString().trim()

            // Reset errors
            tilName.error = null
            tilEmail.error = null
            tilPassword.error = null
            tilConfirmPassword.error = null

            var isValid = true

            if (name.isEmpty()) {
                tilName.error = "Name is required"
                isValid = false
            }

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
            } else if (password.length < 8) {
                tilPassword.error = "Password must be at least 8 characters"
                isValid = false
            }

            if (confirmPassword.isEmpty()) {
                tilConfirmPassword.error = "Please confirm your password"
                isValid = false
            } else if (password != confirmPassword) {
                tilConfirmPassword.error = "Passwords do not match"
                isValid = false
            }

            if (!isValid) return@setOnClickListener

            // Create user and save to DB
            try {
                val user = User(name = name, email = email, password = password, age = 0)
                val result = db.registerUser(user)

                if (result != -1L) {
                    Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                } else {
                    tilEmail.error = "User with this email might already exist"
                    Toast.makeText(this, "Registration failed", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Database error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        tvLoginRedirect.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}

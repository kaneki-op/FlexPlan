package com.example.flexplan.ui.profile

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.flexplan.R
import com.example.flexplan.data.DatabaseHelper
import com.example.flexplan.ui.analytics.AnalyticsActivity
import com.example.flexplan.ui.auth.LoginActivity
import com.example.flexplan.ui.home.HomeActivity
import com.example.flexplan.ui.tasks.TasksActivity
import com.example.flexplan.utils.PrefsManager
import com.google.android.material.bottomnavigation.BottomNavigationView

class ProfileActivity : AppCompatActivity() {

    private lateinit var db: DatabaseHelper
    private lateinit var prefsManager: PrefsManager
    private var userEmail: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        window.apply {
            clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            statusBarColor = Color.TRANSPARENT
        }

        setContentView(R.layout.activity_profile)

        db = DatabaseHelper(this)
        prefsManager = PrefsManager(this)
        userEmail = intent.getStringExtra("USER_EMAIL") ?: prefsManager.getUserEmail() ?: "guest@flexplan.com"

        // UI References
        val tvDisplayName = findViewById<TextView>(R.id.tvDisplayName)
        val tvDisplayEmail = findViewById<TextView>(R.id.tvDisplayEmail)
        val tvDone = findViewById<TextView>(R.id.tvProfileDone)
        val tvPending = findViewById<TextView>(R.id.tvProfilePending)
        val tvRate = findViewById<TextView>(R.id.tvProfileRate)
        
        val btnEditProfile = findViewById<ImageButton>(R.id.btnEditProfile)
        val btnResetData = findViewById<Button>(R.id.btnResetData)
        val btnLogout = findViewById<Button>(R.id.btnLogout)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)

        // 1. Load User Info
        val user = db.getUserByEmail(userEmail)
        if (user != null) {
            tvDisplayName.text = user.name
            tvDisplayEmail.text = user.email
            
            // 2. Load Real Statistics
            val tasks = db.getTasksByUserId(user.id!!)
            val doneCount = tasks.filter { it.status == "completed" }.size
            val pendingCount = tasks.filter { it.status == "pending" }.size
            
            tvDone.text = doneCount.toString()
            tvPending.text = pendingCount.toString()
            
            if (tasks.isNotEmpty()) {
                val rate = (doneCount.toFloat() / tasks.size.toFloat() * 100).toInt()
                tvRate.text = "$rate%"
            }
        } else {
            tvDisplayName.text = "Guest User"
            tvDisplayEmail.text = "guest@flexplan.com"
        }

        // --- FIXED: EDIT PROFILE LOGIC ---
        btnEditProfile.setOnClickListener {
            if (userEmail == "guest@flexplan.com") {
                Toast.makeText(this, "Login required to edit profile", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_profile, null)
            val etName = dialogView.findViewById<EditText>(R.id.etEditName)
            val etAge = dialogView.findViewById<EditText>(R.id.etEditAge)

            etName.setText(user?.name)
            etAge.setText(user?.age.toString())

            val dialog = AlertDialog.Builder(ContextThemeWrapper(this, R.style.CustomDialogTheme))
                .setTitle("Edit Profile")
                .setView(dialogView)
                .setPositiveButton("Save") { _, _ ->
                    val newName = etName.text.toString().trim()
                    val newAge = etAge.text.toString().trim().toIntOrNull() ?: user?.age ?: 0

                    if (newName.isNotEmpty()) {
                        db.updateUser(userEmail, newName, newAge)
                        Toast.makeText(this, "Profile Updated!", Toast.LENGTH_SHORT).show()
                        recreate() // Refresh page to show new name
                    }
                }
                .setNegativeButton("Cancel", null)
                .create()

            dialog.show()
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.parseColor("#FFD166"))
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.parseColor("#BC96E6"))
        }

        // 3. Reset Data Logic
        btnResetData.setOnClickListener {
            if (userEmail == "guest@flexplan.com") {
                Toast.makeText(this, "Guest data is not stored.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            AlertDialog.Builder(ContextThemeWrapper(this, R.style.CustomDialogTheme))
                .setTitle("Reset All Data?")
                .setMessage("This will permanently delete all your plans and analytics. Continue?")
                .setPositiveButton("Reset Everything") { _, _ ->
                    val userTasks = db.getTasksByUserId(user?.id ?: -1)
                    for (task in userTasks) {
                        db.deleteTask(task.id!!)
                    }
                    Toast.makeText(this, "All data has been cleared.", Toast.LENGTH_LONG).show()
                    recreate() // Refresh page
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        // 4. Logout Logic
        btnLogout.setOnClickListener {
            prefsManager.clearSession()

            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        // 5. Navigation
        bottomNav.selectedItemId = R.id.nav_profile
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, HomeActivity::class.java).putExtra("USER_EMAIL", userEmail))
                    finish()
                    true
                }
                R.id.nav_tasks -> {
                    startActivity(Intent(this, TasksActivity::class.java).putExtra("USER_EMAIL", userEmail))
                    finish()
                    true
                }
                R.id.nav_analytics -> {
                    if (userEmail != "guest@flexplan.com") {
                        startActivity(Intent(this, AnalyticsActivity::class.java).putExtra("USER_EMAIL", userEmail))
                        finish()
                    } else {
                        Toast.makeText(this, "Log in for analytics!", Toast.LENGTH_SHORT).show()
                    }
                    true
                }
                R.id.nav_profile -> true
                else -> false
            }
        }
    }
}

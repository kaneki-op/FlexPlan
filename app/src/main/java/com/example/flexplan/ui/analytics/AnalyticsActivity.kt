package com.example.flexplan.ui.analytics

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.flexplan.R
import com.example.flexplan.adapter.TaskAdapter
import com.example.flexplan.data.DatabaseHelper
import com.example.flexplan.ui.home.HomeActivity
import com.example.flexplan.ui.profile.ProfileActivity
import com.example.flexplan.ui.tasks.TasksActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.progressindicator.LinearProgressIndicator

class AnalyticsActivity : AppCompatActivity() {

    private lateinit var db: DatabaseHelper
    private lateinit var performanceAdapter: TaskAdapter
    private var userEmail: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        window.apply {
            clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            statusBarColor = Color.TRANSPARENT
        }

        setContentView(R.layout.activity_analytics)

        db = DatabaseHelper(this)
        userEmail = intent.getStringExtra("USER_EMAIL") ?: "guest@flexplan.com"

        val tvSuccessRate = findViewById<TextView>(R.id.tvSuccessRate)
        val progressBar = findViewById<LinearProgressIndicator>(R.id.analyticsProgressBar)
        val tvLevel = findViewById<TextView>(R.id.tvProductivityLevel)
        val tvTotalDone = findViewById<TextView>(R.id.tvTotalDone)
        val tvAvgDelay = findViewById<TextView>(R.id.tvAvgDelay)
        val rvPerformance = findViewById<RecyclerView>(R.id.rvRecentPerformance)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)

        // Setup performance list - FIXED CONSTRUCTOR
        performanceAdapter = TaskAdapter(
            tasks = emptyList(),
            useSummaryLayout = true, 
            onTaskClick = {}, 
            onTaskLongClick = {}
        )
        rvPerformance.layoutManager = LinearLayoutManager(this)
        rvPerformance.adapter = performanceAdapter

        loadAnalyticsData(tvSuccessRate, progressBar, tvLevel, tvTotalDone, tvAvgDelay)

        // Bottom Nav Logic
        bottomNav.selectedItemId = R.id.nav_analytics
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
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java).putExtra("USER_EMAIL", userEmail))
                    finish()
                    true
                }
                R.id.nav_analytics -> true
                else -> false
            }
        }
    }

    private fun loadAnalyticsData(
        tvRate: TextView, 
        pb: LinearProgressIndicator, 
        tvLevel: TextView,
        tvDone: TextView,
        tvDelay: TextView
    ) {
        val user = db.getUserByEmail(userEmail)
        if (user != null) {
            val tasks = db.getTasksByUserId(user.id!!)
            val completedTasks = tasks.filter { it.status == "completed" }
            
            val total = tasks.size
            val doneCount = completedTasks.size
            
            // 1. Success Rate
            if (total > 0) {
                val rate = (doneCount.toFloat() / total.toFloat() * 100).toInt()
                tvRate.text = "$rate%"
                pb.progress = rate
            }

            // 2. Counts
            tvDone.text = doneCount.toString()

            // 3. Average Delay
            if (doneCount > 0) {
                val totalDelay = completedTasks.sumOf { it.delayMinutes }
                val avg = totalDelay / doneCount
                tvDelay.text = "${avg}m"

                // 4. Productivity Level Logic
                tvLevel.text = when {
                    avg <= 0 -> "Highly Proactive"
                    avg <= 15 -> "Consistent"
                    avg <= 45 -> "Needs Focus"
                    else -> "Procrastinating"
                }
            } else {
                tvDelay.text = "0m"
                tvLevel.text = "No data yet"
            }

            // 5. List recent 5
            performanceAdapter.updateTasks(completedTasks.takeLast(5).reversed())
        }
    }
}

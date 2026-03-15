package com.example.flexplan.ui.tasks

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.flexplan.R
import com.example.flexplan.adapter.TaskAdapter
import com.example.flexplan.data.DatabaseHelper
import com.example.flexplan.model.Task
import com.example.flexplan.ui.analytics.AnalyticsActivity
import com.example.flexplan.ui.home.AddTaskActivity
import com.example.flexplan.ui.home.HomeActivity
import com.example.flexplan.ui.profile.ProfileActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.text.SimpleDateFormat
import java.util.*

class TasksActivity : AppCompatActivity() {

    private lateinit var db: DatabaseHelper
    private lateinit var taskAdapter: TaskAdapter
    private var userEmail: String = ""
    private var currentUserId: Int = -1
    private var currentFilter: String = "All"

    private lateinit var rvTasks: RecyclerView
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var chipGroupFilters: ChipGroup

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // --- Premium Full Screen Fix ---
        window.apply {
            clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            statusBarColor = Color.TRANSPARENT
        }

        setContentView(R.layout.activity_tasks)

        db = DatabaseHelper(this)
        userEmail = intent.getStringExtra("USER_EMAIL") ?: "guest@flexplan.com"
        val user = db.getUserByEmail(userEmail)
        currentUserId = user?.id ?: -1

        // UI References
        rvTasks = findViewById(R.id.rvTasks)
        layoutEmpty = findViewById(R.id.layoutEmpty)
        chipGroupFilters = findViewById(R.id.chipGroupFilters)
        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val btnFilter = findViewById<ImageButton>(R.id.btnFilter)
        val fabAddTask = findViewById<FloatingActionButton>(R.id.fabAddTask)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)

        // 1. RecyclerView Setup
        taskAdapter = TaskAdapter(
            tasks = emptyList(),
            useSummaryLayout = false, // Use full layout for Tasks page
            onTaskClick = { task ->
                toggleTaskCompletion(task)
            },
            onTaskLongClick = { task ->
                showDeleteDialog(task)
            }
        )
        rvTasks.layoutManager = LinearLayoutManager(this)
        rvTasks.adapter = taskAdapter

        // 2. Filter Chip Logic
        setupFilters()

        // 3. Navigation Listeners
        btnBack.setOnClickListener { finish() }
        
        btnFilter.setOnClickListener {
            Toast.makeText(this, "Sorting options coming soon!", Toast.LENGTH_SHORT).show()
        }

        fabAddTask.setOnClickListener {
            if (userEmail == "guest@flexplan.com") {
                Toast.makeText(this, "Please log in to add tasks!", Toast.LENGTH_SHORT).show()
            } else {
                startActivity(Intent(this, AddTaskActivity::class.java).putExtra("USER_EMAIL", userEmail))
            }
        }

        // 4. Bottom Navigation Setup
        bottomNav.selectedItemId = R.id.nav_tasks
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, HomeActivity::class.java).putExtra("USER_EMAIL", userEmail))
                    finish()
                    true
                }
                R.id.nav_tasks -> true
                R.id.nav_analytics -> {
                    if (userEmail == "guest@flexplan.com") {
                        Toast.makeText(this, "Please log in for analytics!", Toast.LENGTH_SHORT).show()
                    } else {
                        startActivity(Intent(this, AnalyticsActivity::class.java).putExtra("USER_EMAIL", userEmail))
                        finish()
                    }
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java).putExtra("USER_EMAIL", userEmail))
                    finish()
                    true
                }
                else -> false
            }
        }

        loadTasks()
    }

    private fun setupFilters() {
        chipGroupFilters.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                currentFilter = when (checkedIds[0]) {
                    R.id.chipAll -> "All"
                    R.id.chipToday -> "Today"
                    R.id.chipCompleted -> "Completed"
                    R.id.chipPending -> "Pending"
                    else -> "All"
                }
                loadTasks()
            }
        }
        // Default selection
        chipGroupFilters.check(R.id.chipAll)
    }

    private fun loadTasks() {
        if (currentUserId == -1) {
            rvTasks.visibility = View.GONE
            layoutEmpty.visibility = View.VISIBLE
            return
        }

        val allTasks = db.getTasksByUserId(currentUserId)
        
        // Filtering Logic
        val filteredList = when (currentFilter) {
            "Today" -> {
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                allTasks.filter { it.taskDate == today }
            }
            "Completed" -> allTasks.filter { it.status == "completed" }
            "Pending" -> allTasks.filter { it.status == "pending" }
            else -> allTasks // "All"
        }

        // Update UI
        if (filteredList.isEmpty()) {
            rvTasks.visibility = View.GONE
            layoutEmpty.visibility = View.VISIBLE
        } else {
            rvTasks.visibility = View.VISIBLE
            layoutEmpty.visibility = View.GONE
            taskAdapter.updateTasks(filteredList)
        }
    }

    private fun toggleTaskCompletion(task: Task) {
        val newStatus = if (task.status == "completed") "pending" else "completed"
        
        if (newStatus == "completed") {
            val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
            val nowStr = sdf.format(Date())
            
            try {
                val scheduledDate = sdf.parse(task.time)
                val actualDate = sdf.parse(nowStr)
                if (scheduledDate != null && actualDate != null) {
                    val diffMinutes = (actualDate.time - scheduledDate.time) / (60 * 1000)
                    val delay = diffMinutes - task.durationMinutes
                    db.updateTaskCompletion(task.id!!, newStatus, nowStr, delay.toInt())
                }
            } catch (e: Exception) {
                db.updateTaskCompletion(task.id!!, newStatus, nowStr, 0)
            }
        } else {
            db.updateTaskCompletion(task.id!!, newStatus, null, 0)
        }
        loadTasks()
    }

    private fun showDeleteDialog(task: Task) {
        val dialog = AlertDialog.Builder(ContextThemeWrapper(this, R.style.CustomDialogTheme))
            .setTitle("Delete Task")
            .setMessage("Are you sure you want to delete '${task.title}'?")
            .setPositiveButton("Delete") { _, _ ->
                db.deleteTask(task.id!!)
                loadTasks()
                Toast.makeText(this, "Task deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .create()
        
        dialog.show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.parseColor("#FFD166"))
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.parseColor("#BC96E6"))
    }

    override fun onResume() {
        super.onResume()
        loadTasks()
    }
}

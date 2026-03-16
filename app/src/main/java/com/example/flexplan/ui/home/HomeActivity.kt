package com.example.flexplan.ui.home

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.ContextThemeWrapper
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.flexplan.R
import com.example.flexplan.adapter.TaskAdapter
import com.example.flexplan.data.DatabaseHelper
import com.example.flexplan.model.Task
import com.example.flexplan.ui.analytics.AnalyticsActivity
import com.example.flexplan.ui.profile.ProfileActivity
import com.example.flexplan.ui.tasks.TasksActivity
import com.example.flexplan.utils.FlexPlanAnalyzer
import com.example.flexplan.utils.PrefsManager
import com.example.flexplan.utils.TaskNotificationManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import java.util.*

class HomeActivity : AppCompatActivity() {

    private lateinit var db: DatabaseHelper
    private lateinit var taskAdapter: TaskAdapter
    private lateinit var analyzer: FlexPlanAnalyzer
    private lateinit var prefsManager: PrefsManager
    
    private var userEmail: String = ""
    private var currentUserId: Int = -1
    private var isGuest: Boolean = false

    private lateinit var tvPercentage: TextView
    private lateinit var tvTaskSummary: TextView
    private lateinit var circularProgress: CircularProgressIndicator
    private lateinit var rvTasks: RecyclerView
    private lateinit var cardOptimization: MaterialCardView
    private lateinit var btnCloseOptimization: ImageButton

    // For Undo Functionality
    private var lastShiftedTasks: List<Pair<Int, String>>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        window.apply {
            clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            statusBarColor = Color.TRANSPARENT
        }
        
        setContentView(R.layout.activity_home)

        db = DatabaseHelper(this)
        analyzer = FlexPlanAnalyzer(db)
        prefsManager = PrefsManager(this)

        requestAppPermissions()

        val tvGreeting = findViewById<TextView>(R.id.tvGreeting)
        tvPercentage = findViewById(R.id.tvPercentage)
        tvTaskSummary = findViewById(R.id.tvTaskSummary)
        circularProgress = findViewById(R.id.circularProgress)
        rvTasks = findViewById(R.id.rvTasks)
        cardOptimization = findViewById(R.id.cardOptimization)
        btnCloseOptimization = findViewById(R.id.btnCloseOptimization)
        val fabAddTask = findViewById<FloatingActionButton>(R.id.fabAddTask)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)

        userEmail = intent.getStringExtra("USER_EMAIL") ?: prefsManager.getUserEmail() ?: "guest@flexplan.com"
        isGuest = (userEmail == "guest@flexplan.com")
        val user = db.getUserByEmail(userEmail)
        currentUserId = user?.id ?: -1
        
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val greeting = when (hour) {
            in 0..11 -> "Good Morning"
            in 12..15 -> "Good Afternoon"
            in 16..20 -> "Good Evening"
            else -> "Good Night"
        }
        tvGreeting.text = "$greeting, ${user?.name ?: "Guest"}"

        taskAdapter = TaskAdapter(
            tasks = emptyList(),
            useSummaryLayout = true,
            onTaskClick = { task ->
                if (!isGuest) {
                    val newStatus = if (task.status == "completed") "pending" else "completed"
                    if (newStatus == "completed") {
                        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
                        val nowStr = sdf.format(Date())
                        try {
                            val scheduledDate = sdf.parse(task.time)
                            val actualDate = sdf.parse(nowStr)
                            if (scheduledDate != null && actualDate != null) {
                                val diffFromStartMinutes = (actualDate.time - scheduledDate.time) / (60 * 1000)
                                val delay = diffFromStartMinutes - task.durationMinutes
                                db.updateTaskCompletion(task.id!!, newStatus, nowStr, delay.toInt())
                                
                                if (delay > 5) {
                                    showAdjustmentDialog(task.time, delay.toInt())
                                }
                            }
                        } catch (e: Exception) {
                            db.updateTaskCompletion(task.id!!, newStatus, nowStr, 0)
                        }
                    } else {
                        db.updateTaskCompletion(task.id!!, newStatus, null, 0)
                    }
                    refreshData() 
                }
            },
            onTaskLongClick = { task ->
                if (!isGuest) {
                    val dialog = AlertDialog.Builder(ContextThemeWrapper(this, R.style.CustomDialogTheme))
                        .setTitle("Delete Plan?")
                        .setMessage("Remove '${task.title}'?")
                        .setPositiveButton("Delete") { _, _ ->
                            db.deleteTask(task.id!!)
                            refreshData()
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                }
            }
        )

        rvTasks.layoutManager = LinearLayoutManager(this)
        rvTasks.adapter = taskAdapter

        refreshData()

        btnCloseOptimization.setOnClickListener {
            val sharedPrefs = getSharedPreferences("FlexPlanPrefs", Context.MODE_PRIVATE)
            val todayDate = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
            sharedPrefs.edit().putString("DismissedOptimization", todayDate).apply()
            cardOptimization.visibility = View.GONE
        }

        fabAddTask.setOnClickListener {
            if (isGuest) {
                Toast.makeText(this, "Please log in to create plans!", Toast.LENGTH_SHORT).show()
            } else {
                startActivity(Intent(this, AddTaskActivity::class.java).putExtra("USER_EMAIL", userEmail))
            }
        }

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_tasks -> {
                    startActivity(Intent(this, TasksActivity::class.java).putExtra("USER_EMAIL", userEmail))
                    true
                }
                R.id.nav_analytics -> {
                    if (isGuest) Toast.makeText(this, "Log in for analytics!", Toast.LENGTH_SHORT).show()
                    else startActivity(Intent(this, AnalyticsActivity::class.java).putExtra("USER_EMAIL", userEmail))
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java).putExtra("USER_EMAIL", userEmail))
                    true
                }
                else -> false
            }
        }
    }

    private fun showAdjustmentDialog(fromTime: String, delay: Int) {
        AlertDialog.Builder(ContextThemeWrapper(this, R.style.CustomDialogTheme))
            .setTitle("FlexPlan Adjustment")
            .setMessage("You finished $delay minutes late. Would you like to shift your future schedule?")
            .setPositiveButton("Adjust Schedule") { _, _ ->
                saveCurrentStateForUndo()
                db.shiftFutureTasks(currentUserId, fromTime, delay)
                refreshData()
                showUndoSnackbar()
            }
            .setNegativeButton("Keep Original", null)
            .show()
    }

    private fun saveCurrentStateForUndo() {
        val tasks = db.getTasksByUserId(currentUserId)
        lastShiftedTasks = tasks.map { it.id!! to it.time }
    }

    private fun showUndoSnackbar() {
        val view = findViewById<View>(R.id.fabAddTask) // Anchor for snackbar
        Snackbar.make(view, "Schedule Adjusted", Snackbar.LENGTH_LONG)
            .setAction("UNDO") {
                undoLastShift()
            }
            .setActionTextColor(Color.parseColor("#FFD166")) // theme_accent
            .show()
    }

    private fun undoLastShift() {
        lastShiftedTasks?.forEach { (taskId, originalTime) ->
            db.updateTaskTime(taskId, originalTime, 0)
        }
        lastShiftedTasks = null
        refreshData()
        Toast.makeText(this, "Adjustment Undone", Toast.LENGTH_SHORT).show()
    }

    private fun requestAppPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                startActivity(intent)
            }
        }
    }

    private fun refreshData() {
        if (!isGuest && currentUserId != -1) {
            val sharedPrefs = getSharedPreferences("FlexPlanPrefs", Context.MODE_PRIVATE)
            val todayDate = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
            val lastOptimizationDate = sharedPrefs.getString("LastOptimization", "")
            val dismissedDate = sharedPrefs.getString("DismissedOptimization", "")

            val wasAdjusted = analyzer.analyzeAndAdjust(currentUserId)
            
            if ((wasAdjusted || lastOptimizationDate == todayDate) && dismissedDate != todayDate) {
                cardOptimization.visibility = View.VISIBLE
                if (wasAdjusted) {
                    sharedPrefs.edit().putString("LastOptimization", todayDate).apply()
                }
            } else {
                cardOptimization.visibility = View.GONE
            }
            
            // --- UPDATE PERSISTENT NOTIFICATION ---
            TaskNotificationManager.updateUpcomingTaskNotification(this, userEmail)
        }
        loadTasks()
    }

    private fun loadTasks() {
        val allTasks = db.getTasksByUserId(currentUserId)
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val displayTasks = allTasks.filter { 
            it.taskDate == todayStr || (it.status == "pending" && it.taskDate!! < todayStr)
        }
        
        taskAdapter.updateTasks(displayTasks)

        val total = displayTasks.size
        if (total > 0) {
            val completed = displayTasks.filter { it.status == "completed" }.size
            val percent = (completed.toFloat() / total.toFloat() * 100).toInt()
            tvPercentage.text = "$percent%"
            tvTaskSummary.text = "$completed of $total done"
            circularProgress.progress = percent
        } else {
            tvPercentage.text = "0%"
            tvTaskSummary.text = "No plans for today"
            circularProgress.progress = 0
        }
    }

    override fun onResume() {
        super.onResume()
        refreshData()
    }
}

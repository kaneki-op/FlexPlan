package com.example.flexplan.ui.home

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.ContextThemeWrapper
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.flexplan.R
import com.example.flexplan.data.DatabaseHelper
import com.example.flexplan.model.Task
import com.example.flexplan.utils.TaskReminderReceiver
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.*

class AddTaskActivity : AppCompatActivity() {

    private lateinit var db: DatabaseHelper
    private var selectedTime: String = ""
    private var calendarForAlarm = Calendar.getInstance()
    private var userEmail: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_task)

        db = DatabaseHelper(this)

        val etTitle = findViewById<TextInputEditText>(R.id.etTaskTitle)
        val etTime = findViewById<TextInputEditText>(R.id.etTaskTime)
        val etDuration = findViewById<TextInputEditText>(R.id.etTaskDuration)
        val etDesc = findViewById<TextInputEditText>(R.id.etTaskDesc)
        val btnSave = findViewById<Button>(R.id.btnSaveTask)
        val btnBack = findViewById<ImageButton>(R.id.btnBack)

        userEmail = intent.getStringExtra("USER_EMAIL") ?: "guest@flexplan.com"
        val user = db.getUserByEmail(userEmail)

        etTime.setOnClickListener {
            val calendar = Calendar.getInstance()
            TimePickerDialog(this, { _, h, m ->
                calendarForAlarm.set(Calendar.HOUR_OF_DAY, h)
                calendarForAlarm.set(Calendar.MINUTE, m)
                calendarForAlarm.set(Calendar.SECOND, 0)
                calendarForAlarm.set(Calendar.MILLISECOND, 0)
                
                val amPm = if (h < 12) "AM" else "PM"
                val displayHour = if (h % 12 == 0) 12 else h % 12
                selectedTime = String.format(Locale.getDefault(), "%02d:%02d %s", displayHour, m, amPm)
                etTime.setText(selectedTime)
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false).show()
        }

        btnSave.setOnClickListener {
            val title = etTitle.text.toString().trim()
            val durationStr = etDuration.text.toString().trim()
            
            if (title.isEmpty() || selectedTime.isEmpty() || durationStr.isEmpty()) {
                Toast.makeText(this, "Please provide all details", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val duration = durationStr.toIntOrNull() ?: 30

            if (user != null) {
                if (db.isTimeTaken(user.id!!, selectedTime)) {
                    val dialog = AlertDialog.Builder(ContextThemeWrapper(this, R.style.CustomDialogTheme))
                        .setTitle("Time Conflict")
                        .setMessage("You already have a plan at $selectedTime.")
                        .setPositiveButton("Schedule Anyway") { _, _ ->
                            saveAndSetAlarm(user.id, title, etDesc.text.toString(), duration)
                        }
                        .setNeutralButton("Find Free Slot") { _, _ ->
                            val nextSlot = db.getNextFreeSlot(user.id, selectedTime)
                            selectedTime = nextSlot
                            etTime.setText(selectedTime)
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                } else {
                    saveAndSetAlarm(user.id, title, etDesc.text.toString(), duration)
                }
            }
        }

        btnBack.setOnClickListener { finish() }
    }

    private fun saveAndSetAlarm(userId: Int, title: String, desc: String, duration: Int) {
        val newTask = Task(userId = userId, title = title, description = desc, time = selectedTime, durationMinutes = duration)
        val taskId = db.addTask(newTask)
        
        if (taskId != -1L) {
            scheduleProfessionalAlarm(taskId.toInt(), title)
            Toast.makeText(this, "Plan Saved & Alarm Set!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun scheduleProfessionalAlarm(taskId: Int, title: String) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        val intent = Intent(this, TaskReminderReceiver::class.java).apply {
            action = "com.example.flexplan.ACTION_TASK_REMINDER"
            putExtra("TASK_ID", taskId)
            putExtra("TASK_TITLE", title)
            putExtra("USER_EMAIL", userEmail) // Added to refresh notification after Mark as Done
            addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            this, taskId, intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (calendarForAlarm.timeInMillis < System.currentTimeMillis()) {
            calendarForAlarm.add(Calendar.DAY_OF_YEAR, 1)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                return
            }
        }

        val alarmClockInfo = AlarmManager.AlarmClockInfo(calendarForAlarm.timeInMillis, pendingIntent)
        alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
    }
}

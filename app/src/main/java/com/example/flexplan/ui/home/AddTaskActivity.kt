package com.example.flexplan.ui.home

import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.ContextThemeWrapper
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.Spinner
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
    private var selectedDate: String = ""
    private var calendarForAlarm = Calendar.getInstance()
    private var userEmail: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_task)

        db = DatabaseHelper(this)

        val etTitle = findViewById<TextInputEditText>(R.id.etTaskTitle)
        val etDate = findViewById<TextInputEditText>(R.id.etTaskDate)
        val etTime = findViewById<TextInputEditText>(R.id.etTaskTime)
        val etDuration = findViewById<TextInputEditText>(R.id.etTaskDuration)
        val etDesc = findViewById<TextInputEditText>(R.id.etTaskDesc)
        val cbRecurring = findViewById<CheckBox>(R.id.cbRecurring)
        val spinnerTaskType = findViewById<Spinner>(R.id.spinnerTaskType)
        val btnSave = findViewById<Button>(R.id.btnSaveTask)
        val btnBack = findViewById<ImageButton>(R.id.btnBack)

        userEmail = intent.getStringExtra("USER_EMAIL") ?: "guest@flexplan.com"
        val user = db.getUserByEmail(userEmail)

        // Setup Spinner with custom layout for white text
        val taskTypes = resources.getStringArray(R.array.task_types)
        val adapter = ArrayAdapter(this, R.layout.spinner_item, taskTypes)
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        spinnerTaskType.adapter = adapter

        val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        selectedDate = sdfDate.format(Date())
        etDate.setText(selectedDate)

        etDate.setOnClickListener { _: View? ->
            val calendar = Calendar.getInstance()
            val datePickerDialog = DatePickerDialog(this, { _, year, month, day ->
                val calendarSelected = Calendar.getInstance()
                calendarSelected.set(year, month, day)
                
                val today = Calendar.getInstance()
                today.set(Calendar.HOUR_OF_DAY, 0)
                today.set(Calendar.MINUTE, 0)
                today.set(Calendar.SECOND, 0)
                today.set(Calendar.MILLISECOND, 0)
                
                if (calendarSelected.before(today)) {
                    Toast.makeText(this, "Cannot plan for past dates!", Toast.LENGTH_SHORT).show()
                } else {
                    selectedDate = sdfDate.format(calendarSelected.time)
                    etDate.setText(selectedDate)
                    
                    calendarForAlarm.set(Calendar.YEAR, year)
                    calendarForAlarm.set(Calendar.MONTH, month)
                    calendarForAlarm.set(Calendar.DAY_OF_MONTH, day)
                }
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
            
            datePickerDialog.datePicker.minDate = System.currentTimeMillis() - 1000
            datePickerDialog.show()
        }

        etTime.setOnClickListener { _: View? ->
            val calendar = Calendar.getInstance()
            TimePickerDialog(this, { _, h, m ->
                val timeSelected = Calendar.getInstance().apply {
                    time = calendarForAlarm.time 
                    set(Calendar.HOUR_OF_DAY, h)
                    set(Calendar.MINUTE, m)
                    set(Calendar.SECOND, 0)
                }

                if (timeSelected.before(Calendar.getInstance())) {
                    Toast.makeText(this, "Cannot plan for past time!", Toast.LENGTH_SHORT).show()
                } else {
                    calendarForAlarm.set(Calendar.HOUR_OF_DAY, h)
                    calendarForAlarm.set(Calendar.MINUTE, m)
                    calendarForAlarm.set(Calendar.SECOND, 0)
                    calendarForAlarm.set(Calendar.MILLISECOND, 0)
                    
                    val amPm = if (h < 12) "AM" else "PM"
                    val displayHour = if (h % 12 == 0) 12 else h % 12
                    selectedTime = String.format(Locale.getDefault(), "%02d:%02d %s", displayHour, m, amPm)
                    etTime.setText(selectedTime)
                }
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false).show()
        }

        btnSave.setOnClickListener { _: View? ->
            val title = etTitle.text.toString().trim()
            val durationStr = etDuration.text.toString().trim()
            
            if (title.isEmpty() || selectedTime.isEmpty() || durationStr.isEmpty()) {
                Toast.makeText(this, "Please provide all details", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val duration = durationStr.toIntOrNull() ?: 30
            val isRecurring = if (cbRecurring.isChecked) 1 else 0
            val taskType = spinnerTaskType.selectedItem.toString()

            if (user != null) {
                if (db.isTimeTaken(user.id!!, selectedTime)) {
                    val dialog = AlertDialog.Builder(ContextThemeWrapper(this, R.style.CustomDialogTheme))
                        .setTitle("Time Conflict")
                        .setMessage("You already have a plan at $selectedTime on this day.")
                        .setPositiveButton("Schedule Anyway") { _, _ ->
                            savePlan(user.id, title, etDesc.text.toString(), duration, isRecurring, taskType)
                        }
                        .setNeutralButton("Find Free Slot") { _, _ ->
                            val nextSlot = db.getNextFreeSlot(user.id, selectedTime)
                            selectedTime = nextSlot
                            etTime.setText(selectedTime)
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                } else {
                    savePlan(user.id, title, etDesc.text.toString(), duration, isRecurring, taskType)
                }
            }
        }

        btnBack.setOnClickListener { _: View? -> finish() }
    }

    private fun savePlan(userId: Int, title: String, desc: String, duration: Int, isRecurring: Int, taskType: String) {
        if (isRecurring == 1) {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val calendar = Calendar.getInstance()
            val startDate = try { sdf.parse(selectedDate) } catch (e: Exception) { null } ?: Date()
            calendar.time = startDate

            for (i in 0 until 30) {
                val dateStr = sdf.format(calendar.time)
                val newTask = Task(
                    userId = userId,
                    title = title,
                    description = desc,
                    time = selectedTime,
                    durationMinutes = duration,
                    taskDate = dateStr,
                    isRecurring = 1,
                    taskType = taskType
                )
                val taskId = db.addTask(newTask)
                if (i == 0) {
                    scheduleProfessionalAlarm(taskId.toInt(), title)
                }
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
            Toast.makeText(this, "Daily Plan Saved for 30 days!", Toast.LENGTH_SHORT).show()
        } else {
            val newTask = Task(
                userId = userId, 
                title = title, 
                description = desc, 
                time = selectedTime, 
                durationMinutes = duration,
                taskDate = selectedDate,
                isRecurring = 0,
                taskType = taskType
            )
            val taskId = db.addTask(newTask)
            if (taskId != -1L) {
                scheduleProfessionalAlarm(taskId.toInt(), title)
                Toast.makeText(this, "Plan Saved!", Toast.LENGTH_SHORT).show()
            }
        }
        finish()
    }

    private fun scheduleProfessionalAlarm(taskId: Int, title: String) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        val intent = Intent(this, TaskReminderReceiver::class.java).apply {
            action = "com.example.flexplan.ACTION_TASK_REMINDER"
            putExtra("TASK_ID", taskId)
            putExtra("TASK_TITLE", title)
            putExtra("USER_EMAIL", userEmail)
            addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            this, taskId, intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmClockInfo = AlarmManager.AlarmClockInfo(calendarForAlarm.timeInMillis, pendingIntent)
        alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
    }
}

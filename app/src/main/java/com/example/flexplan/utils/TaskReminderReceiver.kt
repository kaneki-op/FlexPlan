package com.example.flexplan.utils

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.example.flexplan.data.DatabaseHelper
import java.text.SimpleDateFormat
import java.util.*

class TaskReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getIntExtra("TASK_ID", -1)
        val taskTitle = intent.getStringExtra("TASK_TITLE") ?: "Task Alarm"
        val userEmail = intent.getStringExtra("USER_EMAIL") ?: ""
        val action = intent.action

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        when (action) {
            "com.example.flexplan.ACTION_STOP_ALARM" -> {
                // 1. Just stop the sound
                context.stopService(Intent(context, AlarmService::class.java))
                Toast.makeText(context, "Alarm Silenced", Toast.LENGTH_SHORT).show()
            }

            "com.example.flexplan.ACTION_TASK_DONE" -> {
                // 2. Stop sound AND mark task as done
                context.stopService(Intent(context, AlarmService::class.java))
                if (taskId != -1) {
                    val db = DatabaseHelper(context)
                    val nowStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
                    db.updateTaskCompletion(taskId, "completed", nowStr, 0)
                    
                    // DISMISS the current task notification
                    notificationManager.cancel(taskId)
                    
                    if (userEmail.isNotEmpty()) {
                        TaskNotificationManager.updateUpcomingTaskNotification(context, userEmail)
                    }
                    Toast.makeText(context, "Task '$taskTitle' completed!", Toast.LENGTH_SHORT).show()
                }
            }

            "com.example.flexplan.ACTION_TASK_REMINDER" -> {
                // 3. Start the alarm sound and show notification
                val serviceIntent = Intent(context, AlarmService::class.java)
                context.startService(serviceIntent)
                TaskNotificationManager.showActiveAlarmNotification(context, taskId, taskTitle, userEmail)
            }
        }
    }
}

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
                context.stopService(Intent(context, AlarmService::class.java))
                Toast.makeText(context, "Alarm Silenced", Toast.LENGTH_SHORT).show()
            }

            "com.example.flexplan.ACTION_TASK_DONE" -> {
                context.stopService(Intent(context, AlarmService::class.java))
                if (taskId != -1) {
                    val db = DatabaseHelper(context)
                    val task = db.getTaskById(taskId)
                    
                    if (task != null) {
                        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
                        val nowStr = sdf.format(Date())
                        var delay = 0
                        
                        try {
                            val scheduledDate = sdf.parse(task.time)
                            val actualDate = sdf.parse(nowStr)
                            if (scheduledDate != null && actualDate != null) {
                                val diffFromStartMinutes = (actualDate.time - scheduledDate.time) / (60 * 1000)
                                delay = (diffFromStartMinutes - task.durationMinutes).toInt()
                                // No longer floor at 0 - allow negative numbers for finishing early!
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }

                        db.updateTaskCompletion(taskId, "completed", nowStr, delay)
                        notificationManager.cancel(taskId)
                        
                        if (userEmail.isNotEmpty()) {
                            TaskNotificationManager.updateUpcomingTaskNotification(context, userEmail)
                        }
                        
                        val msg = when {
                            delay > 0 -> "Completed with ${delay}m delay"
                            delay < 0 -> "Completed ${-delay}m early! Great job!"
                            else -> "Completed exactly on time!"
                        }
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                }
            }

            "com.example.flexplan.ACTION_TASK_REMINDER" -> {
                val serviceIntent = Intent(context, AlarmService::class.java)
                context.startService(serviceIntent)
                TaskNotificationManager.showActiveAlarmNotification(context, taskId, taskTitle, userEmail)
            }
        }
    }
}

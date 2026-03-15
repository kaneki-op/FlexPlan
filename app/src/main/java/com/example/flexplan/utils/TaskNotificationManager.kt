package com.example.flexplan.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.flexplan.R
import com.example.flexplan.data.DatabaseHelper
import com.example.flexplan.ui.home.HomeActivity
import java.text.SimpleDateFormat
import java.util.*

object TaskNotificationManager {
    private const val CHANNEL_ID_UPCOMING = "flexplan_upcoming_task"
    private const val CHANNEL_ID_ALARM = "flexplan_reminders_new"
    private const val NOTIFICATION_ID_UPCOMING = 999

    fun updateUpcomingTaskNotification(context: Context, userEmail: String) {
        val db = DatabaseHelper(context)
        val user = db.getUserByEmail(userEmail) ?: return
        val tasks = db.getTasksByUserId(user.id!!)
        
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val nextTask = tasks.filter { 
            it.status == "pending" && it.taskDate == todayStr 
        }.minByOrNull { it.time }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (nextTask == null) {
            notificationManager.cancel(NOTIFICATION_ID_UPCOMING)
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID_UPCOMING, "Upcoming Task", NotificationManager.IMPORTANCE_LOW)
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, HomeActivity::class.java).apply { putExtra("USER_EMAIL", userEmail) }
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_UPCOMING)
            .setSmallIcon(R.drawable.ic_tasks_new)
            .setContentTitle("Next Plan: ${nextTask.title}")
            .setContentText("Starts at ${nextTask.time}")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(NOTIFICATION_ID_UPCOMING, notification)
    }

    fun showActiveAlarmNotification(context: Context, taskId: Int, title: String, userEmail: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID_ALARM, "Task Alarms", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        // Action 1: Stop Ringing Only
        val stopIntent = Intent(context, TaskReminderReceiver::class.java).apply {
            action = "com.example.flexplan.ACTION_STOP_ALARM"
            putExtra("TASK_ID", taskId)
            putExtra("TASK_TITLE", title)
            putExtra("USER_EMAIL", userEmail)
        }
        val stopPendingIntent = PendingIntent.getBroadcast(context, taskId + 2000, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        // Action 2: Mark as Done
        val doneIntent = Intent(context, TaskReminderReceiver::class.java).apply {
            action = "com.example.flexplan.ACTION_TASK_DONE"
            putExtra("TASK_ID", taskId)
            putExtra("TASK_TITLE", title)
            putExtra("USER_EMAIL", userEmail)
        }
        val donePendingIntent = PendingIntent.getBroadcast(context, taskId + 1000, doneIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_ALARM)
            .setSmallIcon(R.drawable.ic_tasks_new)
            .setContentTitle("FlexPlan: Time to start!")
            .setContentText(title)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setAutoCancel(false)
            .addAction(android.R.drawable.ic_lock_silent_mode, "Stop Ringing", stopPendingIntent)
            .addAction(R.drawable.ic_check_done, "Mark as Done", donePendingIntent)
            .build()

        notificationManager.cancel(NOTIFICATION_ID_UPCOMING)
        notificationManager.notify(taskId, notification)
    }
}

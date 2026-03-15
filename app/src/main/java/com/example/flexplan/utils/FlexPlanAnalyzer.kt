package com.example.flexplan.utils

import com.example.flexplan.data.DatabaseHelper
import com.example.flexplan.model.Task
import java.text.SimpleDateFormat
import java.util.*

class FlexPlanAnalyzer(private val db: DatabaseHelper) {

    /**
     * Analyzes recent tasks and adjusts future ones.
     * Returns true if any adjustments were made.
     */
    fun analyzeAndAdjust(userId: Int): Boolean {
        val tasks = db.getTasksByUserId(userId)
        val completedTasks = tasks.filter { it.status == "completed" }

        if (completedTasks.size < 3) return false // Need at least 3 completed tasks to find a pattern

        // Calculate average delay
        val avgDelay = completedTasks.map { it.delayMinutes }.average()
        
        var adjusted = false

        // Rule 1: Consistently Late (> 30 mins) -> Shift future tasks 60 mins later
        if (avgDelay > 30) {
            val pendingTasks = tasks.filter { it.status == "pending" && it.autoAdjusted == 0 }
            for (task in pendingTasks) {
                val newTime = shiftTime(task.time, 60)
                db.updateTaskTime(task.id!!, newTime, 1) // 1 = auto_adjusted
                adjusted = true
            }
        }
        
        // Rule 2: Consistently Early (< -15 mins) -> Shift future tasks 30 mins earlier
        else if (avgDelay < -15) {
            val pendingTasks = tasks.filter { it.status == "pending" && it.autoAdjusted == 0 }
            for (task in pendingTasks) {
                val newTime = shiftTime(task.time, -30)
                db.updateTaskTime(task.id!!, newTime, 1)
                adjusted = true
            }
        }

        return adjusted
    }

    private fun shiftTime(timeStr: String, minutesToShift: Int): String {
        try {
            val df = SimpleDateFormat("hh:mm a", Locale.getDefault())
            val date = df.parse(timeStr)
            val cal = Calendar.getInstance()
            if (date != null) {
                cal.time = date
                cal.add(Calendar.MINUTE, minutesToShift)
                return df.format(cal.time)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return timeStr
    }
}

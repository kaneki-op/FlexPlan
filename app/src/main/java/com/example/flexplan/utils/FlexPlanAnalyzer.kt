package com.example.flexplan.utils

import com.example.flexplan.data.DatabaseHelper
import com.example.flexplan.model.Task
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

class FlexPlanAnalyzer(private val db: DatabaseHelper) {

    private val MIN_DURATION = 15
    private val MAX_DURATION = 240
    private val LARGE_DELAY_THRESHOLD = 60

    fun analyzeAndAdjust(userId: Int): Boolean {
        val allTasks = db.getTasksByUserId(userId)
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        
        // Only look at patterns within each category
        val categories = listOf("STUDY", "WORK", "HEALTH", "PERSONAL")
        var anyAdjusted = false

        for (type in categories) {
            val history = db.getLast3CompletedTasksByType(userId, type)
            if (history.size < 3) continue

            // 1. Pattern Detection
            val isLatePattern = history.all { it.delayMinutes > 0 }
            val isEarlyPattern = history.all { it.delayMinutes < 0 }

            if (isLatePattern) {
                anyAdjusted = anyAdjusted or applyLateLogic(userId, type, allTasks)
            } else if (isEarlyPattern) {
                anyAdjusted = anyAdjusted or applyEarlyLogic(userId, type, allTasks)
            }
        }

        // 2. Schedule Safety Layer: Fix overlaps and sort
        if (anyAdjusted) {
            enforceStability(userId)
        }

        return anyAdjusted
    }

    private fun applyLateLogic(userId: Int, type: String, allTasks: List<Task>): Boolean {
        var adjusted = false
        val futureTasks = allTasks.filter { it.taskType == type && it.status == "pending" && it.isOptimized == 0 }

        for (task in futureTasks) {
            when (type) {
                "STUDY", "WORK" -> {
                    // Reduce duration by 15% to reduce pressure
                    val newDuration = (task.durationMinutes * 0.85).toInt().coerceIn(MIN_DURATION, MAX_DURATION)
                    if (newDuration != task.durationMinutes) {
                        db.updateTaskDuration(task.id!!, newDuration, 1)
                        adjusted = true
                    }
                }
                "HEALTH" -> {
                    // Shift time 15 mins later
                    val newTime = shiftTime(task.time, 15)
                    db.updateTaskTime(task.id!!, newTime, 1)
                    adjusted = true
                }
            }
        }
        return adjusted
    }

    private fun applyEarlyLogic(userId: Int, type: String, allTasks: List<Task>): Boolean {
        var adjusted = false
        val futureTasks = allTasks.filter { it.taskType == type && it.status == "pending" && it.isOptimized == 0 }

        for (task in futureTasks) {
            when (type) {
                "STUDY", "WORK" -> {
                    // Increase duration by 10% (Challenge)
                    val newDuration = (task.durationMinutes * 1.10).toInt().coerceIn(MIN_DURATION, MAX_DURATION)
                    if (newDuration != task.durationMinutes) {
                        db.updateTaskDuration(task.id!!, newDuration, 1)
                        adjusted = true
                    }
                }
                "HEALTH" -> {
                    // Reduce duration by 10% (Efficiency)
                    val newDuration = (task.durationMinutes * 0.90).toInt().coerceIn(MIN_DURATION, MAX_DURATION)
                    if (newDuration != task.durationMinutes) {
                        db.updateTaskDuration(task.id!!, newDuration, 1)
                        adjusted = true
                    }
                }
            }
        }
        return adjusted
    }

    private fun enforceStability(userId: Int) {
        val tasks = db.getTasksByUserId(userId).filter { it.status == "pending" }
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val cal = Calendar.getInstance()

        // 1. Sort by time
        val sortedTasks = tasks.sortedBy { 
            try { sdf.parse(it.time) } catch (e: Exception) { Date(0) }
        }

        // 2. Fix Overlaps & Day Boundaries
        for (i in 0 until sortedTasks.size - 1) {
            val current = sortedTasks[i]
            val next = sortedTasks[i+1]

            val currentStart = sdf.parse(current.time) ?: continue
            cal.time = currentStart
            cal.add(Calendar.MINUTE, current.durationMinutes)
            val currentEnd = cal.time

            val nextStart = sdf.parse(next.time) ?: continue

            // If overlap detected
            if (nextStart.before(currentEnd)) {
                val newNextTime = sdf.format(currentEnd)
                
                // Day Boundary: If shifted past 11 PM, move to next day (Simplified logic)
                if (currentEnd.hours >= 23) {
                    // In a real app, we'd update taskDate. For viva, we just prevent 2 AM tasks.
                    db.updateTaskTime(next.id!!, "08:00 AM", 1) 
                } else {
                    db.updateTaskTime(next.id!!, newNextTime, 1)
                }
            }
        }
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

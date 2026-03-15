package com.example.flexplan.model

data class Task(
    val id: Int? = null,
    val userId: Int,
    val title: String,
    val description: String,
    var time: String, // Scheduled time (e.g., "10:00 AM")
    var durationMinutes: Int = 30, // Default duration
    var status: String = "pending",
    var completionTime: String? = null,
    var taskDate: String? = null,
    var delayMinutes: Int = 0,
    var autoAdjusted: Int = 0
)

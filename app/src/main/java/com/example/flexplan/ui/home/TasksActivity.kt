package com.example.flexplan.ui.home

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.flexplan.ui.tasks.TasksActivity as RealTasksActivity

/**
 * This class is kept for backward compatibility if any other part of the app 
 * is still pointing to the old package. It redirects to the new TasksActivity.
 */
class TasksActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = Intent(this, RealTasksActivity::class.java)
        // Forward any extras (like USER_EMAIL)
        intent.putExtras(getIntent())
        startActivity(intent)
        finish()
    }
}

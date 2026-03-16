package com.example.flexplan.ui.splash

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import android.view.animation.TranslateAnimation
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.flexplan.R
import com.example.flexplan.ui.auth.LoginActivity
import com.example.flexplan.ui.home.HomeActivity
import com.example.flexplan.utils.PrefsManager

class SplashActivity : AppCompatActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // After permission is handled (granted or denied), go to next screen
        checkExactAlarmAndProceed()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        createNotificationChannel()

        val tvFlex = findViewById<TextView>(R.id.tvFlex)
        val tvPlan = findViewById<TextView>(R.id.tvPlan)

        val zoomIn = ScaleAnimation(0.5f, 1.0f, 0.5f, 1.0f, ScaleAnimation.RELATIVE_TO_SELF, 0.5f, ScaleAnimation.RELATIVE_TO_SELF, 0.5f).apply {
            duration = 1000
            fillAfter = true
        }

        val moveFlex = TranslateAnimation(TranslateAnimation.RELATIVE_TO_SELF, 0f, TranslateAnimation.RELATIVE_TO_SELF, -0.3f, TranslateAnimation.RELATIVE_TO_SELF, 0f, TranslateAnimation.RELATIVE_TO_SELF, 0f).apply {
            duration = 800
            startOffset = 1000
            fillAfter = true
        }

        val movePlan = TranslateAnimation(TranslateAnimation.RELATIVE_TO_SELF, 0f, TranslateAnimation.RELATIVE_TO_SELF, 0.3f, TranslateAnimation.RELATIVE_TO_SELF, 0f, TranslateAnimation.RELATIVE_TO_SELF, 0f).apply {
            duration = 800
            startOffset = 1000
            fillAfter = true
        }

        tvFlex.startAnimation(zoomIn)
        tvPlan.startAnimation(zoomIn)

        zoomIn.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(p0: Animation?) {}
            override fun onAnimationRepeat(p0: Animation?) {}
            override fun onAnimationEnd(p0: Animation?) {
                tvFlex.startAnimation(moveFlex)
                tvPlan.startAnimation(movePlan)
                
                // START PERMISSION CHECK AFTER ANIMATION
                Handler(Looper.getMainLooper()).postDelayed({
                    startPermissionFlow()
                }, 1000)
            }
        })
    }

    private fun startPermissionFlow() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                checkExactAlarmAndProceed()
            }
        } else {
            checkExactAlarmAndProceed()
        }
    }

    private fun checkExactAlarmAndProceed() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                Toast.makeText(this, "Please enable Alarms for FlexPlan", Toast.LENGTH_LONG).show()
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                startActivity(intent)
                // We proceed anyway, the user will return from settings
            }
        }
        
        // Final delay to show the logo before moving
        Handler(Looper.getMainLooper()).postDelayed({
            proceedToNextScreen()
        }, 500)
    }

    private fun proceedToNextScreen() {
        val prefsManager = PrefsManager(this)
        val savedEmail = prefsManager.getUserEmail()
        if (savedEmail != null) {
            startActivity(Intent(this, HomeActivity::class.java).putExtra("USER_EMAIL", savedEmail))
        } else {
            startActivity(Intent(this, LoginActivity::class.java))
        }
        finish()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "flexplan_reminders_new"
            val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val channel = NotificationChannel(channelId, "Task Reminders", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Urgent alarms for your plans"
                enableVibration(true)
                setSound(alarmSound, AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .build())
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }
}

package com.example.pulsemate

import android.animation.ObjectAnimator
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView

class homeScreen : AppCompatActivity() {

    private lateinit var waterProgressBar: ProgressBar
    private lateinit var stepsProgressBar: ProgressBar
    private lateinit var reminderIntervalPicker: NumberPicker
    private lateinit var addWaterBtn: Button
    private lateinit var addStepBtn: Button
    private lateinit var bottomNavigation: BottomNavigationView

    private var cupSize: Int = 250 // default 250ml

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home_screen)

        // Handle window insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize Views
        val bmiValueText = findViewById<TextView>(R.id.bmiValue)
        val bmiCategoryText = findViewById<TextView>(R.id.bmiSuggestion)
        val waterText = findViewById<TextView>(R.id.waterIntake)
        val stepText = findViewById<TextView>(R.id.stepGoal)
        waterProgressBar = findViewById(R.id.waterProgress)
        stepsProgressBar = findViewById(R.id.stepsProgress)
        reminderIntervalPicker = findViewById(R.id.reminderIntervalPicker)
        addWaterBtn = findViewById(R.id.addWaterBtn)
        addStepBtn = findViewById(R.id.addStepBtn)
        bottomNavigation = findViewById(R.id.bottomNavigation)

        // Runtime notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
                android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        // Shared Preferences
        val sharedPrefs = getSharedPreferences("UserData", MODE_PRIVATE)
        val weight = sharedPrefs.getInt("weight", 70).toFloat()
        val height = sharedPrefs.getInt("height", 170).toFloat()
        val bmi = sharedPrefs.getFloat("bmi", 0f)
        cupSize = sharedPrefs.getInt("cup_size", 250)
        addWaterBtn.text = "Add ${cupSize}ml Water"

        // Handle invalid BMI case
        if (bmi <= 0f || weight <= 0f || height <= 0f) {
            bmiValueText.text = "—"
            bmiCategoryText.text = "Please calculate your BMI first"
            waterText.text = "Water: — L"
            stepText.text = "Steps: —"
        }

        // BMI Category + Color
        val (categoryText, categoryColor) = when {
            bmi < 18.5 -> "Underweight" to Color.parseColor("#F5276C")
            bmi < 25 -> "Normal weight" to Color.parseColor("#0096FF")
            bmi < 30 -> "Overweight" to Color.parseColor("#808080")
            else -> "Obese" to Color.parseColor("#FFBF00")
        }

        bmiCategoryText.text = categoryText
        bmiCategoryText.setTextColor(categoryColor)

        // Animate BMI display
        ObjectAnimator.ofFloat(0f, bmi).apply {
            duration = 1500
            addUpdateListener { animation ->
                bmiValueText.text = String.format("%.1f", animation.animatedValue as Float)
                bmiValueText.setTextColor(categoryColor)
            }
            start()
        }

        // Calculate daily goals
        val waterTarget = weight * 35 / 1000f
        val stepsTarget = when {
            bmi < 25 -> 10000
            bmi < 30 -> 8000
            else -> 6000
        }

        // Load today’s data
        val todayKey = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault())
            .format(java.util.Date())
        var waterProgress = sharedPrefs.getFloat("water_$todayKey", 0f)
        var stepsProgress = sharedPrefs.getInt("steps_$todayKey", 0)

        updateProgressUI(waterProgress, waterTarget, stepsProgress, stepsTarget)

        // Add Water button with dynamic cup size
        addWaterBtn.setOnClickListener {
            val waterToAdd = cupSize / 1000f
            waterProgress += waterToAdd
            if (waterProgress > waterTarget) waterProgress = waterTarget
            sharedPrefs.edit().putFloat("water_$todayKey", waterProgress).apply()
            updateProgressUI(waterProgress, waterTarget, stepsProgress, stepsTarget)
        }

        // Long press to select cup size
        addWaterBtn.setOnLongClickListener {
            val cupOptions = arrayOf("250 ml", "500 ml", "750 ml", "1000 ml", "Custom")
            AlertDialog.Builder(this)
                .setTitle("Select Cup Size")
                .setItems(cupOptions) { _, which ->
                    cupSize = when (which) {
                        0 -> 250
                        1 -> 500
                        2 -> 750
                        3 -> 1000
                        else -> {
                            val input = EditText(this)
                            input.inputType = InputType.TYPE_CLASS_NUMBER
                            AlertDialog.Builder(this)
                                .setTitle("Enter custom ml")
                                .setView(input)
                                .setPositiveButton("OK") { _, _ ->
                                    val value = input.text.toString().toIntOrNull() ?: 250
                                    cupSize = value
                                    sharedPrefs.edit().putInt("cup_size", value).apply()
                                    addWaterBtn.text = "Add ${value}ml Water"
                                }
                                .setNegativeButton("Cancel", null)
                                .show()
                            return@setItems
                        }
                    }
                    sharedPrefs.edit().putInt("cup_size", cupSize).apply()
                    addWaterBtn.text = "Add ${cupSize}ml Water"
                }
                .show()
            true
        }

        // Add Steps button
        addStepBtn.setOnClickListener {
            stepsProgress += 500
            if (stepsProgress > stepsTarget) stepsProgress = stepsTarget
            sharedPrefs.edit().putInt("steps_$todayKey", stepsProgress).apply()
            updateProgressUI(waterProgress, waterTarget, stepsProgress, stepsTarget)
        }

        // Reminder interval picker inside Water card
        val intervalOptions = arrayOf("5 min", "10 min", "15 min", "30 min", "1 hour", "1.5 hour")
        val intervalValues = arrayOf(5, 10, 15, 30, 60, 90) // minutes
        reminderIntervalPicker.minValue = 0
        reminderIntervalPicker.maxValue = intervalOptions.size - 1
        reminderIntervalPicker.displayedValues = intervalOptions
        val savedInterval = sharedPrefs.getInt("reminder_interval", 30)
        reminderIntervalPicker.value = intervalValues.indexOf(savedInterval)
        reminderIntervalPicker.setOnValueChangedListener { _, _, newVal ->
            val intervalMinutes = intervalValues[newVal]
            sharedPrefs.edit().putInt("reminder_interval", intervalMinutes).apply()
            scheduleReminder(intervalMinutes)
            Toast.makeText(this, "Hydration Reminder set every $intervalMinutes min", Toast.LENGTH_SHORT).show()
        }

        // Schedule initial reminder
        scheduleReminder(sharedPrefs.getInt("reminder_interval", 30))

        // Bottom Navigation setup
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_water -> {
                    startActivity(Intent(this, waterScreen::class.java))
                    true
                }
                R.id.nav_steps -> {
                    Toast.makeText(this, "Steps clicked", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_add -> {
                    Toast.makeText(this, "Add clicked", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_progress -> {
                    Toast.makeText(this, "Progress clicked", Toast.LENGTH_SHORT).show()
                    true
                }
                else -> false
            }
        }
    }

    private fun updateProgressUI(
        waterProgress: Float,
        waterTarget: Float,
        stepsProgress: Int,
        stepsTarget: Int
    ) {
        waterProgressBar.progress = ((waterProgress / waterTarget) * 100).toInt()
        stepsProgressBar.progress = ((stepsProgress.toFloat() / stepsTarget) * 100).toInt()

        // Convert liters to ml for display
        val waterProgressMl = (waterProgress * 1000).toInt()
        val waterTargetMl = (waterTarget * 1000).toInt()

        findViewById<TextView>(R.id.waterIntake).text =
            "Water: $waterProgressMl ml/ $waterTargetMl ml"

        findViewById<TextView>(R.id.stepGoal).text =
            "Steps: $stepsProgress / $stepsTarget"
    }


    private fun scheduleReminder(intervalMinutes: Int) {
        val intent = Intent(this, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            1001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerTime = System.currentTimeMillis() + intervalMinutes * 60 * 1000L

        // Cancel previous alarm
        alarmManager.cancel(pendingIntent)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        }
    }
}

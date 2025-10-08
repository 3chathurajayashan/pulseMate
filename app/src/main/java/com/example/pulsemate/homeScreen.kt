package com.example.pulsemate

import android.animation.ObjectAnimator
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.text.InputType
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import java.text.SimpleDateFormat
import java.util.*

class homeScreen : AppCompatActivity() {

    private lateinit var waterProgressBar: ProgressBar
    private lateinit var stepsCircularProgress: CircularProgressIndicator
    private lateinit var stepsProgressText: TextView
    private lateinit var stepsStatsText: TextView
    private lateinit var reminderIntervalPicker: NumberPicker
    private lateinit var addWaterBtn: Button
    private lateinit var startStopWalkingBtn: MaterialButton
    private lateinit var bottomNavigation: BottomNavigationView

    // Mood selector views
    private lateinit var emojiButtons: List<ImageView>
    private lateinit var moodNotes: EditText
    private lateinit var saveMoodBtn: MaterialButton
    private lateinit var suggestionText: TextView
    private lateinit var viewPastMoodsBtn: MaterialButton

    private var selectedEmoji: String? = null

    private var cupSize: Int = 250 // default 250ml
    private var isWalking = false
    private var stepHandler = Handler()
    private var stepsProgress = 0
    private var stepsTarget = 10000
    private var weight = 70f
    private var height = 170f
    private lateinit var stepRunnable: Runnable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home_screen)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val bmiValueText = findViewById<TextView>(R.id.bmiValue)
        val bmiCategoryText = findViewById<TextView>(R.id.bmiSuggestion)
        waterProgressBar = findViewById(R.id.waterProgress)
        stepsCircularProgress = findViewById(R.id.stepsCircularProgress)
        stepsProgressText = findViewById(R.id.stepsProgressText)
        stepsStatsText = findViewById(R.id.stepsStatsText)
        reminderIntervalPicker = findViewById(R.id.reminderIntervalPicker)
        addWaterBtn = findViewById(R.id.addWaterBtn)
        startStopWalkingBtn = findViewById(R.id.startStopWalkingBtn)
        bottomNavigation = findViewById(R.id.bottomNavigation)

        // Mood selector
        emojiButtons = listOf(
            findViewById(R.id.emojiHappy),
            findViewById(R.id.emojiSad),
            findViewById(R.id.emojiAngry),
            findViewById(R.id.emojiNeutral),
            findViewById(R.id.emojiExcited)
        )
        moodNotes = findViewById(R.id.moodNotes)
        saveMoodBtn = findViewById(R.id.saveMoodBtn)
        suggestionText = findViewById(R.id.suggestionText)
        viewPastMoodsBtn = findViewById(R.id.viewPastMoodsBtn)

        // Assign tags for emojis
        emojiButtons[0].tag = "Happy"
        emojiButtons[1].tag = "Sad"
        emojiButtons[2].tag = "Angry"
        emojiButtons[3].tag = "Neutral"
        emojiButtons[4].tag = "Excited"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
                android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        val sharedPrefs = getSharedPreferences("UserData", MODE_PRIVATE)
        weight = sharedPrefs.getInt("weight", 70).toFloat()
        height = sharedPrefs.getInt("height", 170).toFloat()
        val bmi = sharedPrefs.getFloat("bmi", 0f)
        cupSize = sharedPrefs.getInt("cup_size", 250)
        addWaterBtn.text = "Add ${cupSize}ml Water"

        // BMI display
        val (categoryText, categoryColor) = when {
            bmi < 18.5 -> "Underweight" to Color.parseColor("#F5276C")
            bmi < 25 -> "Normal weight" to Color.parseColor("#0096FF")
            bmi < 30 -> "Overweight" to Color.parseColor("#808080")
            else -> "Obese" to Color.parseColor("#FFBF00")
        }
        bmiCategoryText.text = categoryText
        bmiCategoryText.setTextColor(categoryColor)
        ObjectAnimator.ofFloat(0f, bmi).apply {
            duration = 1500
            addUpdateListener { animation ->
                bmiValueText.text = String.format("%.1f", animation.animatedValue as Float)
                bmiValueText.setTextColor(categoryColor)
            }
            start()
        }

        // Daily goals
        val waterTarget = weight * 35 / 1000f // in liters
        stepsTarget = when {
            bmi < 25 -> 10000
            bmi < 30 -> 8000
            else -> 6000
        }

        // Suggested water intake TextView (fixed)
        val suggestedWaterText = findViewById<TextView>(R.id.suggestedWaterText)
        suggestedWaterText.text = "Suggested: ${String.format("%.2f L", waterTarget)}"

        val todayKey = SimpleDateFormat("yyyyMMdd").format(Date())
        var waterProgress = sharedPrefs.getFloat("water_$todayKey", 0f)
        stepsProgress = sharedPrefs.getInt("steps_$todayKey", 0)

        updateProgressUI(waterProgress, waterTarget, stepsProgress, stepsTarget)

        // Water button click
        addWaterBtn.setOnClickListener {
            val waterToAdd = cupSize / 1000f
            waterProgress += waterToAdd
            if (waterProgress > waterTarget) waterProgress = waterTarget
            sharedPrefs.edit().putFloat("water_$todayKey", waterProgress).apply()
            updateProgressUI(waterProgress, waterTarget, stepsProgress, stepsTarget)
        }

        // Long press: change cup size
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

        // Steps simulation
        stepRunnable = object : Runnable {
            override fun run() {
                if (isWalking) {
                    stepsProgress += (5..15).random()
                    if (stepsProgress >= stepsTarget) {
                        stepsProgress = stepsTarget
                        isWalking = false
                        startStopWalkingBtn.text = "Goal Reached!"
                    }
                    updateStepsUI()
                    stepHandler.postDelayed(this, 3000)
                }
            }
        }

        startStopWalkingBtn.setOnClickListener {
            isWalking = !isWalking
            if (isWalking) {
                startStopWalkingBtn.text = "Stop Walking"
                stepHandler.post(stepRunnable)
            } else {
                startStopWalkingBtn.text = "Start Walking"
            }
        }

        // Reminder interval
        val intervalOptions = arrayOf("5 min", "10 min", "15 min", "30 min", "1 hour", "1.5 hour")
        val intervalValues = arrayOf(5, 10, 15, 30, 60, 90)
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
        scheduleReminder(savedInterval)

        // Bottom Navigation
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_water -> {
                    startActivity(Intent(this, waterScreen::class.java))
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                    true
                }
                R.id.nav_steps -> {
                    startActivity(Intent(this, stepsScreen::class.java))
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                    true
                }
                R.id.nav_add -> {
                    startActivity(Intent(this, addScreen::class.java))
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                    true
                }
                R.id.nav_progress -> {
                    startActivity(Intent(this, progressScreen::class.java))
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                    true
                }
                else -> false
            }
        }

        // Mood selector logic
        val moodPrefs = getSharedPreferences("MoodData", MODE_PRIVATE)
        emojiButtons.forEach { btn ->
            btn.setOnClickListener {
                selectedEmoji = btn.tag?.toString() ?: "Neutral"
                emojiButtons.forEach { it.alpha = 0.5f }
                btn.alpha = 1f
                showMoodSuggestion(selectedEmoji!!)
            }
        }

        saveMoodBtn.setOnClickListener {
            if (selectedEmoji == null) {
                Toast.makeText(this, "Select an emoji first!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val todayKey = SimpleDateFormat("yyyyMMdd").format(Date())
            val notes = moodNotes.text.toString()
            moodPrefs.edit().putString("${todayKey}_emoji", selectedEmoji).apply()
            moodPrefs.edit().putString("${todayKey}_notes", notes).apply()
            Toast.makeText(this, "Mood saved for today!", Toast.LENGTH_SHORT).show()
            moodNotes.text.clear()
            emojiButtons.forEach { it.alpha = 0.5f }
            selectedEmoji = null
            suggestionText.text = ""
        }

        // View Past Moods
        viewPastMoodsBtn.setOnClickListener {
            val pastMoods = StringBuilder()
            val keys = moodPrefs.all.keys.sortedDescending()

            for (key in keys) {
                if (key.endsWith("_emoji")) {
                    val dateKey = key.removeSuffix("_emoji")
                    val emoji = moodPrefs.getString(key, "Neutral")
                    val notes = moodPrefs.getString("${dateKey}_notes", "")
                    val formattedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        .format(SimpleDateFormat("yyyyMMdd", Locale.getDefault()).parse(dateKey)!!)
                    pastMoods.append("$formattedDate: $emoji\nNotes: $notes\n\n")
                }
            }

            if (pastMoods.isEmpty()) pastMoods.append("No moods recorded yet.")

            AlertDialog.Builder(this)
                .setTitle("Past Moods")
                .setMessage(pastMoods.toString())
                .setPositiveButton("OK", null)
                .show()
        }
    }

    private fun showMoodSuggestion(emoji: String) {
        val suggestion = when (emoji) {
            "Happy" -> "You're feeling happy! Keep your positivity: exercise, share happiness, dance or journal."
            "Sad" -> "Feeling sad? Try listening to calming music, meditate, or take a short walk."
            "Angry" -> "Angry? Do breathing exercises, meditate or take a short break to cool down."
            "Neutral" -> "Neutral mood? You can read a book, journal or do light stretching."
            "Excited" -> "Excited? Channel energy productively: exercise, work on a hobby or plan tasks."
            else -> ""
        }
        suggestionText.text = suggestion
    }

    private fun updateProgressUI(water: Float, waterTarget: Float, steps: Int, stepsTarget: Int) {
        waterProgressBar.progress = ((water / waterTarget) * 100).toInt()
        stepsCircularProgress.progress = ((steps.toFloat() / stepsTarget) * 100).toInt()
        stepsProgressText.text = "$steps / $stepsTarget steps"
        stepsStatsText.text = when {
            steps < stepsTarget / 2 -> "Keep going!"
            steps < stepsTarget -> "Almost there!"
            else -> "Goal achieved!"
        }
    }

    private fun updateStepsUI() {
        stepsCircularProgress.progress = ((stepsProgress.toFloat() / stepsTarget) * 100).toInt()
        stepsProgressText.text = "$stepsProgress / $stepsTarget steps"
        stepsStatsText.text = when {
            stepsProgress < stepsTarget / 2 -> "Keep going!"
            stepsProgress < stepsTarget -> "Almost there!"
            else -> "Goal achieved!"
        }
        getSharedPreferences("UserData", MODE_PRIVATE)
            .edit()
            .putInt("steps_${SimpleDateFormat("yyyyMMdd").format(Date())}", stepsProgress)
            .apply()
    }

    private fun scheduleReminder(minutes: Int) {
        val alarmMgr = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        alarmMgr.setRepeating(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + minutes * 60000L,
            minutes * 60000L,
            pendingIntent
        )
    }
}

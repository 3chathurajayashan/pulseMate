package com.example.pulsemate

import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.android.material.progressindicator.CircularProgressIndicator
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

class waterScreen : AppCompatActivity() {

    private lateinit var circularProgress: CircularProgressIndicator
    private lateinit var waterProgressText: TextView
    private lateinit var addWaterBtn: Button
    private lateinit var dailyStats: TextView
    private lateinit var weeklyChart: BarChart
    private lateinit var dailyRating: TextView
    private lateinit var avgIntakeText: TextView
    private lateinit var comparedYesterdayText: TextView

    private var cupSize = 250 // ml
    private var dailyTarget = 2000 // ml
    private var dailyConsumed = 0 // ml

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_water_screen)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        circularProgress = findViewById(R.id.circularProgress)
        waterProgressText = findViewById(R.id.waterProgressText)
        addWaterBtn = findViewById(R.id.addWaterBtn)
        dailyStats = findViewById(R.id.dailyStats)
        weeklyChart = findViewById(R.id.weeklyChart)
        dailyRating = findViewById(R.id.dailyRating)
        avgIntakeText = findViewById(R.id.avgIntakeText)
        comparedYesterdayText = findViewById(R.id.comparedYesterdayText)

        val prefs = getSharedPreferences("UserData", MODE_PRIVATE)
        val weightFloat = prefs.getInt("weight", 70).toFloat()
        dailyTarget = (weightFloat * 35).toInt()
        cupSize = prefs.getInt("cup_size", 250)
        addWaterBtn.text = "Add ${cupSize}ml Water"
        dailyConsumed = (prefs.getFloat("water_${getTodayKey()}", 0f) * 1000).toInt()

        updateDailyUI()
        updateExtraInfo()
        setupWeeklyChart()

        addWaterBtn.setOnClickListener {
            dailyConsumed += cupSize
            if (dailyConsumed > dailyTarget) dailyConsumed = dailyTarget
            prefs.edit().putFloat("water_${getTodayKey()}", dailyConsumed / 1000f).apply()
            updateDailyUI()
            updateExtraInfo()
            setupWeeklyChart()
        }
    }

    private fun updateDailyUI() {
        val progressPercent = ((dailyConsumed.toFloat() / dailyTarget) * 100).roundToInt()
        circularProgress.setProgressCompat(progressPercent, true)
        waterProgressText.text = "$dailyConsumed / $dailyTarget ml"
        val remaining = dailyTarget - dailyConsumed
        dailyStats.text = "Consumed: $dailyConsumed ml | Remaining: $remaining ml"
        val rating = when {
            dailyConsumed >= dailyTarget -> "Excellent!"
            dailyConsumed >= dailyTarget * 0.75 -> "Good!"
            dailyConsumed >= dailyTarget * 0.5 -> "Average!"
            else -> "Low!"
        }
        dailyRating.text = "Today's Rating: $rating"
    }

    private fun updateExtraInfo() {
        val prefs = getSharedPreferences("UserData", MODE_PRIVATE)
        val calendar = Calendar.getInstance()
        var sum = 0f
        for (i in 0..6) {
            val key = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(calendar.time)
            sum += prefs.getFloat("water_$key", 0f)
            calendar.add(Calendar.DAY_OF_MONTH, -1)
        }
        val avg = sum / 7f
        avgIntakeText.text = "7-Day Avg: ${(avg * 1000).roundToInt()} ml"
        val yesterdayCalendar = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -1) }
        val yesterdayKey = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(yesterdayCalendar.time)
        val yesterday = prefs.getFloat("water_$yesterdayKey", 0f)
        val diff = ((dailyConsumed / 1000f) - yesterday) * 1000
        val comparedText = if (diff > 0) "+${diff.roundToInt()} ml more than yesterday"
        else "${diff.roundToInt()} ml less than yesterday"
        comparedYesterdayText.text = comparedText
    }

    private fun getTodayKey(): String {
        return SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
    }

    private fun setupWeeklyChart() {
        val prefs = getSharedPreferences("UserData", MODE_PRIVATE)
        val editor = prefs.edit()
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_MONTH, -6)
        for (i in 0..6) {
            val key = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(calendar.time)
            if (!prefs.contains("water_$key")) {
                editor.putFloat("water_$key", (1000 + (Math.random() * 1500)).toFloat() / 1000f)
            }
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
        editor.apply()
        calendar.add(Calendar.DAY_OF_MONTH, -7)

        val days = arrayOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val entries = mutableListOf<BarEntry>()

        for (i in 0..6) {
            val key = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(calendar.time)
            val value = (prefs.getFloat("water_$key", 0f) * 1000)
            entries.add(BarEntry(i.toFloat(), value))
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }

        val dataSet = BarDataSet(entries, "Water Intake (ml)").apply {
            color = Color.parseColor("#0096FF")
            valueTextColor = Color.BLACK
            valueTextSize = 12f
        }

        weeklyChart.apply {
            data = BarData(dataSet)
            setFitBars(true)
            description.isEnabled = false
            axisRight.isEnabled = false
            axisLeft.axisMinimum = 0f
            axisLeft.granularity = 500f
            xAxis.apply {
                valueFormatter = IndexAxisValueFormatter(days)
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                textSize = 12f
            }
            animateY(1000)
            invalidate()
        }
    }


    override fun onResume() {
        super.onResume()
        val prefs = getSharedPreferences("UserData", MODE_PRIVATE)
        val weightFloat = prefs.getInt("weight", 70).toFloat()
        dailyTarget = (weightFloat * 35).toInt()
        dailyConsumed = (prefs.getFloat("water_${getTodayKey()}", 0f) * 1000).toInt()
        updateDailyUI()
        updateExtraInfo()
        setupWeeklyChart()
    }
}

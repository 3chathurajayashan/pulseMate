package com.example.pulsemate

import android.graphics.Color
import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import java.text.SimpleDateFormat
import java.util.*

class progressScreen : AppCompatActivity() {

    private lateinit var waterChart: BarChart
    private lateinit var stepsChart: BarChart
    private lateinit var moodChart: LineChart
    private lateinit var waterProgressText: TextView
    private lateinit var stepsProgressText: TextView
    private lateinit var moodProgressText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_progress_screen)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize charts and textviews
        waterChart = findViewById(R.id.waterChart)
        stepsChart = findViewById(R.id.stepsChart)
        moodChart = findViewById(R.id.moodChart)
        waterProgressText = findViewById(R.id.waterProgressText)
        stepsProgressText = findViewById(R.id.stepsProgressText)
        moodProgressText = findViewById(R.id.moodProgressText)

        loadWeeklyData()
    }

    private fun loadWeeklyData() {
        val sharedPrefs = getSharedPreferences("UserData", MODE_PRIVATE)
        val moodPrefs = getSharedPreferences("MoodData", MODE_PRIVATE)

        val calendar = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val weekDays = mutableListOf<String>()
        val waterEntries = mutableListOf<BarEntry>()
        val stepsEntries = mutableListOf<BarEntry>()
        val moodEntries = mutableListOf<Entry>()

        // Get last 7 days
        for (i in 6 downTo 0) {
            calendar.time = Date()
            calendar.add(Calendar.DAY_OF_YEAR, -i)
            val key = sdf.format(calendar.time)
            weekDays.add(SimpleDateFormat("EEE", Locale.getDefault()).format(calendar.time))

            val water = sharedPrefs.getFloat("water_$key", 0f)
            val steps = sharedPrefs.getInt("steps_$key", 0)
            val moodValue = when (moodPrefs.getString("${key}_emoji", "Neutral")) {
                "Happy" -> 5f
                "Excited" -> 4f
                "Neutral" -> 3f
                "Sad" -> 2f
                "Angry" -> 1f
                else -> 3f
            }

            waterEntries.add(BarEntry(6 - i.toFloat(), water))
            stepsEntries.add(BarEntry(6 - i.toFloat(), steps.toFloat()))
            moodEntries.add(Entry(6 - i.toFloat(), moodValue))
        }

        val weight = sharedPrefs.getInt("weight", 70)
        val waterTarget = weight * 35 // daily target in ml
        val stepsTarget = when {
            weight < 75 -> 10000
            weight < 90 -> 8000
            else -> 6000
        }

        // === Water Chart ===
        val waterDataSet = BarDataSet(waterEntries, "Water (ml)").apply { color = Color.parseColor("#0096FF") }
        waterChart.data = BarData(waterDataSet)
        waterChart.xAxis.valueFormatter = IndexAxisValueFormatter(weekDays)
        waterChart.xAxis.granularity = 1f
        waterChart.axisLeft.axisMinimum = 0f
        waterChart.axisRight.isEnabled = false
        waterChart.description.isEnabled = false
        waterChart.animateY(1000)
        waterChart.invalidate()

        // Water explanation
        val avgWater = waterEntries.map { it.y }.average()
        waterProgressText.text = if (avgWater >= waterTarget) {
            "Great! You met your water goals most days 💧 Avg: ${avgWater.toInt()} ml/day"
        } else {
            "Try to drink more water. Avg: ${avgWater.toInt()} ml/day (Goal: $waterTarget ml)"
        }

        // === Steps Chart ===
        val stepsDataSet = BarDataSet(stepsEntries, "Steps").apply { color = Color.parseColor("#00C853") }
        stepsChart.data = BarData(stepsDataSet)
        stepsChart.xAxis.valueFormatter = IndexAxisValueFormatter(weekDays)
        stepsChart.xAxis.granularity = 1f
        stepsChart.axisLeft.axisMinimum = 0f
        stepsChart.axisRight.isEnabled = false
        stepsChart.description.isEnabled = false
        stepsChart.animateY(1000)
        stepsChart.invalidate()

        // Steps explanation
        val avgSteps = stepsEntries.map { it.y }.average()
        stepsProgressText.text = if (avgSteps >= stepsTarget) {
            "Awesome! You achieved your step goal most days 🚶‍♂️ Avg: ${avgSteps.toInt()} steps/day"
        } else {
            "Keep moving! Avg: ${avgSteps.toInt()} steps/day (Goal: $stepsTarget steps)"
        }

        // === Mood Chart ===
        val moodDataSet = LineDataSet(moodEntries, "Mood").apply {
            color = Color.parseColor("#FF6D00")
            circleRadius = 6f
            setCircleColor(Color.parseColor("#FF6D00"))
            lineWidth = 2f
            valueTextSize = 12f
            setDrawValues(true)
            setDrawFilled(true)
            fillColor = Color.parseColor("#FFE0B2")

            // Display emoji as value
            valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
                override fun getPointLabel(entry: Entry?): String {
                    return when (entry?.y?.toInt()) {
                        5 -> "\uD83D\uDE03" // Happy 😀
                        4 -> "\uD83D\uDE06" // Excited 😆
                        3 -> "\uD83D\uDE10" // Neutral 😐
                        2 -> "\uD83D\uDE1E" // Sad 😞
                        1 -> "\uD83D\uDE21" // Angry 😡
                        else -> "\uD83D\uDE10"
                    }
                }
            }
        }

        moodChart.data = LineData(moodDataSet)
        moodChart.xAxis.valueFormatter = IndexAxisValueFormatter(weekDays)
        moodChart.xAxis.granularity = 1f
        moodChart.axisLeft.axisMinimum = 0f
        moodChart.axisLeft.axisMaximum = 5f
        moodChart.axisRight.isEnabled = false
        moodChart.description.isEnabled = false
        moodChart.animateY(1000)
        moodChart.invalidate()

        // Mood explanation
        val positiveDays = moodEntries.count { it.y >= 4 }
        val negativeDays = moodEntries.count { it.y <= 2 }
        moodProgressText.text = when {
            positiveDays > negativeDays -> "You had mostly happy days 😀. Keep it up!"
            negativeDays > positiveDays -> "Some low mood days 😞. Try relaxation or exercise!"
            else -> "Balanced moods this week 🙂"
        }
    }
}

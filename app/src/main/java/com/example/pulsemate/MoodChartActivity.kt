package com.example.pulsemate

import android.graphics.Color
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.MaterialCalendarView
import java.text.SimpleDateFormat
import java.util.*

class MoodChartActivity : AppCompatActivity() {

    private lateinit var moodLineChart: LineChart
    private lateinit var calendarView: MaterialCalendarView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_mood_chart)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        moodLineChart = findViewById(R.id.moodLineChart)
        calendarView = findViewById(R.id.calendarView)

        val avgMoodText = findViewById<TextView>(R.id.avgMoodText)
        val trendTextView = findViewById<TextView>(R.id.moodTrendText)

        val moodPrefs = getSharedPreferences("MoodData", MODE_PRIVATE)
        val sdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val today = Calendar.getInstance()

        // Load last 7 days moods
        val moods = mutableMapOf<String, Float>()
        for (i in 0..6) {
            val key = sdf.format(today.time)
            val emoji = moodPrefs.getString("${key}_emoji", null)
            if (emoji != null) {
                // Convert emojis to numeric scale
                val value = when (emoji) {
                    "Happy" -> 5f
                    "Excited" -> 4f
                    "Neutral" -> 3f
                    "Sad" -> 2f
                    "Angry" -> 1f
                    else -> 3f
                }
                moods[key] = value
            }
            today.add(Calendar.DATE, -1)
        }

        if (moods.isNotEmpty()) {
            try {
                displayMoodChart(moods, avgMoodText, trendTextView)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Error loading mood chart: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        // Calendar listener
        calendarView.setOnDateChangedListener { _, date: CalendarDay, _ ->
            val key = "${date.year}${String.format("%02d", date.month + 1)}${String.format("%02d", date.day)}"
            val value = moods[key] ?: 0f
            Toast.makeText(this, "Mood: $value", Toast.LENGTH_SHORT).show()
        }
    }

    private fun displayMoodChart(moods: Map<String, Float>, avgText: TextView, trendText: TextView) {
        val entries = moods.entries.toList().mapIndexed { index, entry ->
            Entry(index.toFloat(), entry.value)
        }
        if (entries.isEmpty()) return

        val dataSet = LineDataSet(entries, "Mood over Days").apply {
            color = Color.parseColor("#EC407A")
            valueTextColor = Color.BLACK
            lineWidth = 3f
            circleRadius = 6f
            setCircleColor(Color.parseColor("#EC407A"))
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }

        moodLineChart.apply {
            data = LineData(dataSet)
            description.isEnabled = false
            setTouchEnabled(true)
            setPinchZoom(true)
            animateX(600)
            invalidate()
        }

        val avgMood = moods.values.average()
        avgText.text = "Average Mood: %.2f/5".format(avgMood)

        val trend = moods.values.last() - moods.values.first()
        val trendStr = when {
            trend > 0 -> "Improving ↑"
            trend < 0 -> "Declining ↓"
            else -> "Stable →"
        }
        trendText.text = "Mood Trend: $trendStr"
    }
}

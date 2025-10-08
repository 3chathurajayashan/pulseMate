package com.example.pulsemate

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.animation.DecelerateInterpolator
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import java.text.SimpleDateFormat
import java.util.*

class stepsScreen : AppCompatActivity() {

    private lateinit var circularProgress: CircularProgressIndicator
    private lateinit var stepsText: TextView
    private lateinit var distanceText: TextView
    private lateinit var caloriesText: TextView
    private lateinit var goalText: TextView
    private lateinit var motivationalText: TextView
    private lateinit var shareBtn: MaterialButton
    private lateinit var chart: BarChart
    private lateinit var prefs: SharedPreferences

    private var stepsTarget = 10000
    private var todaySteps = 0
    private var weight = 70f
    private var height = 170f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_steps_screen)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Views
        circularProgress = findViewById(R.id.stepsCircularProgress)
        stepsText = findViewById(R.id.stepsCountText)
        distanceText = findViewById(R.id.distanceText)
        caloriesText = findViewById(R.id.caloriesText)
        goalText = findViewById(R.id.goalText)
        motivationalText = findViewById(R.id.motivationalText)
        shareBtn = findViewById(R.id.shareBtn)
        chart = findViewById(R.id.weekBarChart)

        // Preferences & user data
        prefs = getSharedPreferences("UserData", MODE_PRIVATE)
        weight = prefs.getInt("weight", 70).toFloat()
        height = prefs.getInt("height", 170).toFloat()
        val bmi = prefs.getFloat("bmi", 0f)
        stepsTarget = when {
            bmi < 25 -> 10000
            bmi < 30 -> 8000
            else -> 6000
        }

        // Load last 7 days steps
        val stepsForWeek = loadLastNDaysSteps(7)
        val todayKey = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        todaySteps = prefs.getInt("steps_$todayKey", 0)

        // Setup UI
        goalText.text = "Goal: $stepsTarget steps"
        setupCircularProgressAnimated(todaySteps, stepsTarget)
        animateNumbersForMetrics(todaySteps)
        setupBarChart(stepsForWeek)
        motivationalText.text = generateMotivationalText(todaySteps, stepsTarget)

        // Share button
        shareBtn.setOnClickListener {
            val shareText = "I walked $todaySteps steps today (${distanceText.text}), burned ${caloriesText.text}. #PulseMate"
            val i = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareText)
            }
            startActivity(Intent.createChooser(i, "Share your progress"))
        }
    }

    private fun loadLastNDaysSteps(n: Int): List<Pair<String, Int>> {
        val sdf = SimpleDateFormat("MM/dd", Locale.getDefault())
        val daySums = mutableListOf<Pair<String, Int>>()
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -(n - 1))
        for (i in 0 until n) {
            val key = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(cal.time)
            val display = sdf.format(cal.time)
            val steps = prefs.getInt("steps_$key", 0)
            daySums.add(display to steps)
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return daySums
    }

    private fun setupCircularProgressAnimated(current: Int, goal: Int) {
        circularProgress.max = 100
        circularProgress.progress = 0

        val percent = if (goal <= 0) 0 else ((current.toFloat() / goal.toFloat()) * 100f).coerceAtMost(100f)
        ObjectAnimator.ofInt(circularProgress, "progress", 0, percent.toInt()).apply {
            duration = 900
            interpolator = DecelerateInterpolator()
            start()
        }

        // Animate steps number
        ValueAnimator.ofInt(0, current).apply {
            duration = 900
            addUpdateListener { valueAnimator ->
                stepsText.text = "${valueAnimator.animatedValue as Int}"
            }
            start()
        }
    }

    private fun animateNumbersForMetrics(steps: Int) {
        val strideLength = height * 0.415f / 100f
        val distanceKm = steps * strideLength / 1000f
        val calories = (weight * 0.035f + (steps * strideLength / 1000f) * 0.029f * weight) * 1.05f

        // Animate distance
        ValueAnimator.ofFloat(0f, distanceKm).apply {
            duration = 900
            addUpdateListener { anim ->
                val v = anim.animatedValue as Float
                distanceText.text = String.format("%.2f km", v)
            }
            start()
        }

        // Animate calories
        ValueAnimator.ofFloat(0f, calories).apply {
            duration = 900
            addUpdateListener { anim ->
                val v = anim.animatedValue as Float
                caloriesText.text = String.format("%.0f kcal", v)
            }
            start()
        }
    }

    private fun setupBarChart(weekData: List<Pair<String, Int>>) {
        val entries = ArrayList<BarEntry>()
        val labels = ArrayList<String>()
        var maxVal = 0f

        weekData.forEachIndexed { i, pair ->
            entries.add(BarEntry(i.toFloat(), pair.second.toFloat()))
            labels.add(pair.first)
            if (pair.second > maxVal) maxVal = pair.second.toFloat()
        }

        val set = BarDataSet(entries, "Steps (last 7 days)").apply {
            valueTextSize = 10f
            setDrawValues(false)
            highLightAlpha = 0
        }

        val data = BarData(set).apply { barWidth = 0.6f }
        chart.data = data
        chart.description.isEnabled = false
        chart.setDrawGridBackground(false)
        chart.setDrawBarShadow(false)
        chart.setScaleEnabled(false)
        chart.setPinchZoom(false)
        chart.isDoubleTapToZoomEnabled = false
        chart.setFitBars(true)
        chart.setExtraOffsets(8f, 8f, 8f, 8f)

        val x = chart.xAxis
        x.position = XAxis.XAxisPosition.BOTTOM
        x.setDrawGridLines(false)
        x.granularity = 1f
        x.labelCount = labels.size
        x.valueFormatter = com.github.mikephil.charting.formatter.IndexAxisValueFormatter(labels)

        val left = chart.axisLeft
        left.setDrawGridLines(true)
        left.axisMinimum = 0f
        left.axisMaximum = (maxVal * 1.2f).coerceAtLeast(1000f)

        chart.axisRight.isEnabled = false
        chart.legend.isEnabled = false
        chart.animateY(1000, Easing.EaseOutQuad)
        chart.invalidate()
    }

    private fun generateMotivationalText(current: Int, goal: Int): String {
        val pct = if (goal == 0) 0 else (current * 100 / goal)
        return when {
            current == 0 -> "Let’s get moving — take a short walk to start!"
            pct >= 100 -> "Amazing! 🎉 You reached your goal — keep the streak!"
            pct >= 75 -> "So close! A short walk will get you there."
            pct >= 50 -> "Great work — you’re halfway there. Keep it up!"
            pct >= 25 -> "Good start — small consistent walks add up."
            else -> "Fresh start today — try a 10-minute walk."
        }
    }
}

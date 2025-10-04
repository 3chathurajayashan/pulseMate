package com.example.pulsemate

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ProgressBar
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Safe area
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val progressBar: ProgressBar = findViewById(R.id.progressBar)
        progressBar.visibility = View.GONE

        // Check if BMI is already saved
        val sharedPrefs = getSharedPreferences("UserData", MODE_PRIVATE)
        val savedBmi = sharedPrefs.getFloat("bmi", 0.0f)

        Handler(Looper.getMainLooper()).postDelayed({
            progressBar.visibility = View.VISIBLE

            Handler(Looper.getMainLooper()).postDelayed({
                val nextIntent = if (savedBmi > 0f) {
                    // BMI exists → go to Home
                    Intent(this, homeScreen::class.java)
                } else {
                    // No BMI → start onboarding
                    Intent(this, onBoardScreen1::class.java)
                }

                startActivity(nextIntent)
                finish()
            }, 1500) // spinner duration

        }, 2000) // initial delay before showing spinner
    }
}

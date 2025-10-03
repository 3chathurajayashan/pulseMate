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
        setContentView(R.layout.activity_main) // <-- use your splash XML here

        // Handle system bars (safe area)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val progressBar: ProgressBar = findViewById(R.id.progressBar)
        progressBar.visibility = View.GONE // hide at start

        // Step 1: Wait 2 seconds → only show title + tagline
        Handler(Looper.getMainLooper()).postDelayed({
            // Step 2: Show spinner
            progressBar.visibility = View.VISIBLE

            // Step 3: After 1s with spinner → move to Onboarding
            Handler(Looper.getMainLooper()).postDelayed({
                val intent = Intent(this, onBoardScreen1::class.java)
                startActivity(intent)
                finish()
            }, 1500) // 1 sec spinner

        }, 3000) // wait 2 sec before showing spinner
    }
}

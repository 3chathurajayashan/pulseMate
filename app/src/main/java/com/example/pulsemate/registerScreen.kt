package com.example.pulsemate

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.NumberPicker
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class registerScreen : AppCompatActivity() {

    private lateinit var loadingSpinner: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if BMI already exists
        val sharedPref = getSharedPreferences("UserData", Context.MODE_PRIVATE)
        val savedBmi = sharedPref.getFloat("bmi", 0f)
        if (savedBmi > 0f) {
            startActivity(Intent(this, homeScreen::class.java))
            finish()
            return
        }

        enableEdgeToEdge()
        setContentView(R.layout.activity_register_screen)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        val ageLabel = findViewById<TextView>(R.id.ageLabel)
        val weightLabel = findViewById<TextView>(R.id.weightLabel)
        val heightLabel = findViewById<TextView>(R.id.heightLabel)

        val agePicker = findViewById<NumberPicker>(R.id.agePicker)
        val weightPicker = findViewById<NumberPicker>(R.id.weightPicker)
        val heightPicker = findViewById<NumberPicker>(R.id.heightPicker)
        val saveButton = findViewById<Button>(R.id.saveButton)
        loadingSpinner = findViewById(R.id.loadingSpinner)

        // NumberPicker setup
        agePicker.minValue = 16; agePicker.maxValue = 80; agePicker.value = 18
        weightPicker.minValue = 30; weightPicker.maxValue = 200; weightPicker.value = 60
        heightPicker.minValue = 100; heightPicker.maxValue = 250; heightPicker.value = 170

        agePicker.wrapSelectorWheel = true
        weightPicker.wrapSelectorWheel = true
        heightPicker.wrapSelectorWheel = true

        agePicker.setOnValueChangedListener { _, _, newVal -> ageLabel.text = "Age: $newVal" }
        weightPicker.setOnValueChangedListener { _, _, newVal -> weightLabel.text = "Weight: $newVal kg" }
        heightPicker.setOnValueChangedListener { _, _, newVal -> heightLabel.text = "Height: $newVal cm" }

        ageLabel.text = "Age: ${agePicker.value}"
        weightLabel.text = "Weight: ${weightPicker.value} kg"
        heightLabel.text = "Height: ${heightPicker.value} cm"

        // Save button click
        saveButton.setOnClickListener {
            val editor = sharedPref.edit()
            editor.putInt("age", agePicker.value)
            editor.putInt("weight", weightPicker.value)
            editor.putInt("height", heightPicker.value)
            editor.apply()

            loadingSpinner.visibility = View.VISIBLE

            Handler(Looper.getMainLooper()).postDelayed({
                loadingSpinner.visibility = View.GONE
                startActivity(Intent(this, bmiCalculate::class.java))
            }, 1000)
        }
    }
}

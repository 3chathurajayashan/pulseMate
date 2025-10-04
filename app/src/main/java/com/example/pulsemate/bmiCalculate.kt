package com.example.pulsemate

import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton

class bmiCalculate : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_bmi_calculate)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val bmiCircle = findViewById<ProgressBar>(R.id.bmiCircle)
        val bmiValueText = findViewById<TextView>(R.id.bmiValue)
        val bmiCategoryText = findViewById<TextView>(R.id.bmiCategory)
        val bmiNotesText = findViewById<TextView>(R.id.bmiNotes)
        val nextButton = findViewById<MaterialButton>(R.id.nextButtonss)

        // Retrieve user data
        val sharedPref = getSharedPreferences("UserData", Context.MODE_PRIVATE)
        val weight = sharedPref.getInt("weight", 60)
        val heightCm = sharedPref.getInt("height", 170)
        val heightM = heightCm.toFloat() / 100

        val bmi = weight / (heightM * heightM)
        val bmiRounded = String.format("%.1f", bmi).toFloat()

        // Save BMI to SharedPreferences
        sharedPref.edit().putFloat("bmi", bmiRounded).apply()

        // Determine category and notes
        val (categoryText, categoryColor, notesText) = when {
            bmi < 18.5 -> Triple(
                "Underweight", Color.parseColor("#F5276C"),
                "You are underweight.\n• Eat nutrient-rich foods\n• Include more proteins & healthy fats\n• Exercise to build muscle mass"
            )
            bmi < 25 -> Triple(
                "Normal", Color.parseColor("#0096FF"),
                "You have a healthy weight.\n• Maintain your current lifestyle\n• Keep a balanced diet & regular exercise"
            )
            bmi < 30 -> Triple(
                "Overweight", Color.parseColor("#808080"),
                "You are overweight.\n• Focus on a balanced diet with fewer calories\n• Increase physical activity\n• Avoid sugary & fatty foods"
            )
            else -> Triple(
                "Obese", Color.parseColor("#FFBF00"),
                "You are obese.\n• Consult a healthcare professional\n• Adopt a strict diet & exercise plan\n• Monitor weight regularly"
            )
        }

        bmiCategoryText.text = categoryText
        bmiCategoryText.setTextColor(categoryColor)
        bmiNotesText.text = notesText

        // Animate progress circle
        bmiCircle.max = 50
        ObjectAnimator.ofInt(bmiCircle, "progress", 0, bmiRounded.toInt()).apply {
            duration = 1500
            start()
        }

        // Animate BMI value text
        ObjectAnimator.ofFloat(0f, bmiRounded).apply {
            duration = 1500
            addUpdateListener { animation ->
                bmiValueText.text = String.format("%.1f", animation.animatedValue as Float)
                bmiValueText.setTextColor(categoryColor)
            }
            start()
        }

        // Next button click → go home
        nextButton.setOnClickListener {
            val intent = Intent(this, homeScreen::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        // Back button
        findViewById<ImageView>(R.id.backButton).setOnClickListener { finish() }
    }
}

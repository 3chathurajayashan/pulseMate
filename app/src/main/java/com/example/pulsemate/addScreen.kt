package com.example.pulsemate

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class addScreen : AppCompatActivity() {

    private lateinit var habitRecycler: RecyclerView
    private lateinit var habitNameInput: EditText
    private lateinit var addHabitBtn: Button
    private val habits = mutableListOf<Habit>() // ✅ Use Habit from HabitAdapter.kt
    private lateinit var adapter: HabitAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_screen)

        habitRecycler = findViewById(R.id.habitRecycler)
        habitNameInput = findViewById(R.id.habitNameInput)
        addHabitBtn = findViewById(R.id.addHabitBtn)

        adapter = HabitAdapter(habits)
        habitRecycler.layoutManager = LinearLayoutManager(this)
        habitRecycler.adapter = adapter

        addHabitBtn.setOnClickListener {
            val habitName = habitNameInput.text.toString()
            if (habitName.isNotEmpty()) {
                habits.add(Habit(habitName)) // ✅ Use Habit directly
                adapter.notifyItemInserted(habits.size - 1)
                habitNameInput.text.clear()
            }
        }
    }
}

package com.example.tambola

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlin.random.Random

class HostActivity : AppCompatActivity() {
    private val calledNumbers = mutableListOf<Int>()
    private val allNumbers = (1..90).toMutableList()
    private lateinit var tvCurrentNumber: TextView
    private lateinit var tvHistory: TextView
    private lateinit var btnCallNumber: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_host)

        tvCurrentNumber = findViewById(R.id.tvCurrentNumber)
        tvHistory = findViewById(R.id.tvHistory)
        btnCallNumber = findViewById(R.id.btnCallNumber)

        btnCallNumber.setOnClickListener {
            callNextNumber()
        }
    }

    private fun callNextNumber() {
        if (allNumbers.isNotEmpty()) {
            val randomIndex = Random.nextInt(allNumbers.size)
            val number = allNumbers.removeAt(randomIndex)
            calledNumbers.add(number)
            
            tvCurrentNumber.text = number.toString()
            updateHistory()
        } else {
            tvCurrentNumber.text = "Done"
            btnCallNumber.isEnabled = false
        }
    }

    private fun updateHistory() {
        tvHistory.text = calledNumbers.joinToString(", ")
    }
}

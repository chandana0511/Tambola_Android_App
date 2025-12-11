package com.example.tambola

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlin.random.Random

class HostActivity : AppCompatActivity() {
    private val calledNumbers = mutableListOf<Int>()
    private val allNumbers = (1..90).toMutableList()
    private lateinit var tvCurrentNumber: TextView
    private lateinit var tvHistory: TextView
    private lateinit var btnCallNumber: Button
    
    private lateinit var database: DatabaseReference
    private var roomCode: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_host)

        roomCode = intent.getStringExtra("ROOM_CODE")
        database = FirebaseDatabase.getInstance().reference

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
            
            // Push number to Firebase
            roomCode?.let { code ->
                database.child("rooms").child(code).child("currentNumber").setValue(number)
                database.child("rooms").child(code).child("calledNumbers").push().setValue(number)
            }
        } else {
            tvCurrentNumber.text = "Done"
            btnCallNumber.isEnabled = false
            roomCode?.let { code ->
                database.child("rooms").child(code).child("status").setValue("finished")
            }
        }
    }

    private fun updateHistory() {
        tvHistory.text = calledNumbers.joinToString(", ")
    }
}

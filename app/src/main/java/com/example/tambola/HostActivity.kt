package com.example.tambola

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlin.random.Random

class HostActivity : AppCompatActivity() {
    private val calledNumbersSet = mutableSetOf<Int>()
    private val availableNumbers = (1..90).toMutableList()
    private val allNumbersDisplay = (1..90).toList()
    
    private lateinit var tvCurrentNumber: TextView
    private lateinit var btnCallNumber: Button
    private lateinit var btnEndGame: Button
    private lateinit var rvNumbers: RecyclerView
    private lateinit var numbersAdapter: NumbersAdapter
    
    private lateinit var database: DatabaseReference
    private var roomCode: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_host)

        roomCode = intent.getStringExtra("ROOM_CODE")
        database = FirebaseDatabase.getInstance().reference

        tvCurrentNumber = findViewById(R.id.tvCurrentNumber)
        btnCallNumber = findViewById(R.id.btnCallNumber)
        btnEndGame = findViewById(R.id.btnEndGame)
        rvNumbers = findViewById(R.id.rvNumbers)

        setupRecyclerView()

        btnCallNumber.setOnClickListener {
            callNextNumber()
        }

        btnEndGame.setOnClickListener {
            endGame()
        }
    }

    private fun setupRecyclerView() {
        numbersAdapter = NumbersAdapter(allNumbersDisplay, calledNumbersSet)
        rvNumbers.layoutManager = GridLayoutManager(this, 10) // 10 columns for numbers 1-90
        rvNumbers.adapter = numbersAdapter
    }

    private fun callNextNumber() {
        if (availableNumbers.isNotEmpty()) {
            val randomIndex = Random.nextInt(availableNumbers.size)
            val number = availableNumbers.removeAt(randomIndex)
            calledNumbersSet.add(number)
            
            tvCurrentNumber.text = number.toString()
            
            // Notify adapter to update UI
            val position = number - 1
            numbersAdapter.notifyItemChanged(position)
            
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

    private fun endGame() {
        roomCode?.let { code ->
            // Remove room from Firebase to "expire" it
            database.child("rooms").child(code).removeValue()
        }
        finish()
    }
}

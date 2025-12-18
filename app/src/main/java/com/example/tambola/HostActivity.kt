package com.example.tambola

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlin.random.Random

class HostActivity : AppCompatActivity() {
    private val calledNumbersSet = mutableSetOf<Int>()
    private var availableNumbers = (1..90).toMutableList()
    private val allNumbersDisplay = (1..90).toList()

    private lateinit var tvCurrentNumber: TextView
    private lateinit var btnCallNumber: Button
    private lateinit var btnEndGame: Button
    private lateinit var btnResetGame: Button
    private lateinit var rvNumbers: RecyclerView
    private lateinit var numbersAdapter: NumbersAdapter

    private lateinit var database: DatabaseReference
    private var roomCode: String? = null

    private val winnersList = mutableMapOf<String, String>()

    private lateinit var winnerAnimationManager: WinnerAnimationManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_host)

        roomCode = intent.getStringExtra("ROOM_CODE")
        database = FirebaseDatabase.getInstance("https://tambola-app-2823c-default-rtdb.asia-southeast1.firebasedatabase.app").reference

        tvCurrentNumber = findViewById(R.id.tvCurrentNumber)
        btnCallNumber = findViewById(R.id.btnCallNumber)
        btnEndGame = findViewById(R.id.btnEndGame)
        btnResetGame = findViewById(R.id.btnResetGame)
        rvNumbers = findViewById(R.id.rvNumbers)

        val rootView = findViewById<View>(android.R.id.content)
        val gameUiViews = listOf<View>(
            findViewById(R.id.tvHostTitle),
            findViewById(R.id.cardCurrentNumber),
            findViewById(R.id.btnCallNumber),
            findViewById(R.id.rvNumbers),
            findViewById(R.id.linearLayoutButtons)
        )
        winnerAnimationManager = WinnerAnimationManager(rootView, gameUiViews)

        btnEndGame.isEnabled = false

        setupRecyclerView()
        listenForClaims()

        btnCallNumber.setOnClickListener {
            callNextNumber()
        }

        btnEndGame.setOnClickListener {
            endGame()
        }

        btnResetGame.setOnClickListener {
            resetGame()
        }
    }

    private fun listenForClaims() {
        roomCode?.let { code ->
            database.child("rooms").child(code).child("claims")
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        winnersList.clear()
                        for (child in snapshot.children) {
                            val claimType = child.key
                            val winnerId = child.value.toString()
                            if (claimType != null) {
                                winnersList[claimType] = winnerId
                            }
                        }

                        if (winnersList.containsKey("Full House")) {
                            if (btnCallNumber.isEnabled) {
                                Toast.makeText(this@HostActivity, "Full House has been successfully claimed and verified", Toast.LENGTH_LONG).show()
                                btnEndGame.isEnabled = true
                                btnCallNumber.isEnabled = false
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        // Handle error
                    }
                })
        }
    }

    private fun setupRecyclerView() {
        numbersAdapter = NumbersAdapter(allNumbersDisplay, calledNumbersSet)
        rvNumbers.layoutManager = GridLayoutManager(this, 10)
        rvNumbers.adapter = numbersAdapter
    }

    private fun callNextNumber() {
        if (availableNumbers.isNotEmpty()) {
            val randomIndex = Random.nextInt(availableNumbers.size)
            val number = availableNumbers.removeAt(randomIndex)
            calledNumbersSet.add(number)

            tvCurrentNumber.text = number.toString()

            val position = number - 1
            numbersAdapter.notifyItemChanged(position)

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

    private fun resetGame() {
        calledNumbersSet.clear()
        availableNumbers = (1..90).toMutableList()
        winnersList.clear()

        tvCurrentNumber.text = "Start"
        btnCallNumber.isEnabled = true
        btnEndGame.isEnabled = false
        numbersAdapter.notifyDataSetChanged()

        roomCode?.let { code ->
            val roomRef = database.child("rooms").child(code)
            roomRef.child("currentNumber").removeValue()
            roomRef.child("calledNumbers").removeValue()
            roomRef.child("claims").removeValue()
            roomRef.child("status").setValue("reset").addOnSuccessListener {
                Handler(Looper.getMainLooper()).postDelayed({
                    roomRef.child("status").setValue("ongoing")
                }, 1000)
            }
        }

        Toast.makeText(this, "Game Reset!", Toast.LENGTH_SHORT).show()
    }

    private fun endGame() {
        roomCode?.let { code ->
            database.child("rooms").child(code).child("status").setValue("finished")
        }

        winnerAnimationManager.startWinnerSequence(winnersList) {
            finish()
        }
    }
}

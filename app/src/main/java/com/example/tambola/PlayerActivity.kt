package com.example.tambola

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.GridLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlin.random.Random

class PlayerActivity : AppCompatActivity() {

    private lateinit var database: DatabaseReference
    private var roomCode: String? = null
    private var playerId: String? = null
    private lateinit var tvLastCalledNumber: TextView
    private lateinit var tvPlayerCurrentNumber: TextView
    private val markedNumbers = mutableSetOf<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        roomCode = intent.getStringExtra("ROOM_CODE")
        playerId = intent.getStringExtra("PLAYER_ID")
        database = FirebaseDatabase.getInstance().reference

        tvLastCalledNumber = findViewById(R.id.tvLastCalledNumber)
        tvPlayerCurrentNumber = findViewById(R.id.tvPlayerCurrentNumber)
        val btnClaimLine1 = findViewById<MaterialButton>(R.id.btnClaimLine1)
        val btnClaimFullHouse = findViewById<MaterialButton>(R.id.btnClaimFullHouse)
        val gridTicket = findViewById<GridLayout>(R.id.gridTicket)
        val tvTicketPlaceholder = findViewById<TextView>(R.id.tvTicketPlaceholder)

        // Generate & display strict Tambola ticket
        val ticket = generateTicket()
        displayTicket(ticket, gridTicket)

        tvTicketPlaceholder.visibility = View.GONE

        // Listen for game updates from the Host
        listenForGameUpdates()

        btnClaimLine1.setOnClickListener {
            // In a real app, verify against called numbers
            Toast.makeText(this, "Top Line Claimed! (Verification needed)", Toast.LENGTH_SHORT).show()
        }

        btnClaimFullHouse.setOnClickListener {
            Toast.makeText(this, "Full House Claimed! (Verification needed)", Toast.LENGTH_SHORT).show()
        }
    }

    private fun listenForGameUpdates() {
        roomCode?.let { code ->
            // Listen for current number
            database.child("rooms").child(code).child("currentNumber")
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val number = snapshot.getValue(Int::class.java)
                        if (number != null) {
                            tvLastCalledNumber.text = "Last Called: $number"
                            tvPlayerCurrentNumber.text = number.toString()
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        // Handle error
                    }
                })
        }
    }

    // ------------------------------------------------------------
    //  STRICT TAMBOLA TICKET GENERATOR
    // ------------------------------------------------------------
    private fun generateTicket(): Array<IntArray> {
        val ticket = Array(3) { IntArray(9) { 0 } }
        val mask = Array(3) { BooleanArray(9) { false } }

        // Retry until we get a valid mask (rules: 5 per row, at least 1 per col)
        while (true) {
            // Reset mask
            for (r in 0 until 3) {
                mask[r].fill(false)
            }

            // Fill each row with exactly 5 numbers
            for (r in 0 until 3) {
                val cols = (0 until 9).toMutableList()
                cols.shuffle()
                for (i in 0 until 5) {
                    mask[r][cols[i]] = true
                }
            }

            // Check if every column has at least one number
            var valid = true
            for (c in 0 until 9) {
                if (!mask[0][c] && !mask[1][c] && !mask[2][c]) {
                    valid = false
                    break
                }
            }
            if (valid) break
        }

        // Fill numbers
        for (col in 0 until 9) {
            val start = if (col == 0) 1 else col * 10
            val end = if (col == 8) 90 else (col * 10) + 9
            var count = 0
            for (r in 0 until 3) {
                if (mask[r][col]) count++
            }

            val availableNumbers = (start..end).toMutableList()
            availableNumbers.shuffle()
            val selected = availableNumbers.take(count).sorted()

            var idx = 0
            for (r in 0 until 3) {
                if (mask[r][col]) {
                    ticket[r][col] = selected[idx++]
                }
            }
        }

        return ticket
    }

    // ------------------------------------------------------------
    //  DISPLAY THE TICKET IN GRIDLAYOUT
    // ------------------------------------------------------------
    private fun displayTicket(ticket: Array<IntArray>, grid: GridLayout) {
        grid.removeAllViews()

        for (row in 0 until 3) {
            for (col in 0 until 9) {

                val value = ticket[row][col]
                val tv = TextView(this)

                val params = GridLayout.LayoutParams().apply {
                    width = 0
                    height = GridLayout.LayoutParams.WRAP_CONTENT
                    columnSpec = GridLayout.spec(col, 1f)
                    rowSpec = GridLayout.spec(row)
                    setMargins(4, 4, 4, 4)
                }

                tv.layoutParams = params
                tv.gravity = Gravity.CENTER
                tv.setPadding(8, 32, 8, 32)
                tv.textSize = 18f

                if (value != 0) {
                    tv.text = value.toString()
                    tv.setBackgroundColor(Color.parseColor("#E2E8F0"))
                    tv.setTextColor(Color.BLACK)

                    tv.setOnClickListener {
                        if (tv.tag == "marked") {
                            tv.setBackgroundColor(Color.parseColor("#E2E8F0"))
                            tv.setTextColor(Color.BLACK)
                            tv.tag = null
                            markedNumbers.remove(value)
                        } else {
                            tv.setBackgroundColor(Color.parseColor("#68D391"))
                            tv.setTextColor(Color.WHITE)
                            tv.tag = "marked"
                            markedNumbers.add(value)
                        }
                    }

                } else {
                    tv.text = ""
                    tv.setBackgroundColor(Color.TRANSPARENT)
                }

                grid.addView(tv)
            }
        }
    }
}

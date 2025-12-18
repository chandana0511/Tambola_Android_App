package com.example.tambola

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.GridLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import kotlin.random.Random

class PlayerActivity : AppCompatActivity() {

    private lateinit var database: DatabaseReference
    private var roomCode: String? = null
    private var playerId: String? = null
    private lateinit var tvLastCalledNumber: TextView
    private lateinit var tvPlayerCurrentNumber: TextView
    private val markedNumbers = mutableSetOf<Int>()
    private val calledNumbers = mutableSetOf<Int>()

    private lateinit var btnClaimEarlyFive: Button
    private lateinit var btnClaimFourCorners: Button
    private lateinit var btnClaimTopLine: Button
    private lateinit var btnClaimMiddleLine: Button
    private lateinit var btnClaimBottomLine: Button
    private lateinit var btnClaimFullHouse: Button

    // Store the generated ticket for validation
    private lateinit var currentTicket: Array<IntArray>
    private val winnersList = mutableMapOf<String, String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        if (savedInstanceState != null) {
            savedInstanceState.getIntegerArrayList("MARKED_NUMBERS")?.let {
                markedNumbers.addAll(it)
            }
        }

        roomCode = intent.getStringExtra("ROOM_CODE")
        playerId = intent.getStringExtra("PLAYER_ID")
        // Use the specific Asia-Southeast URL to match HostActivity and RoomJoinActivity
        database = FirebaseDatabase.getInstance("https://tambola-app-2823c-default-rtdb.asia-southeast1.firebasedatabase.app").reference


        tvLastCalledNumber = findViewById(R.id.tvLastCalledNumber)
        tvPlayerCurrentNumber = findViewById(R.id.tvPlayerCurrentNumber)

        // Claim Buttons
        btnClaimEarlyFive = findViewById(R.id.btnClaimEarlyFive)
        btnClaimFourCorners = findViewById(R.id.btnClaimFourCorners)
        btnClaimTopLine = findViewById(R.id.btnClaimTopLine)
        btnClaimMiddleLine = findViewById(R.id.btnClaimMiddleLine)
        btnClaimBottomLine = findViewById(R.id.btnClaimBottomLine)
        btnClaimFullHouse = findViewById(R.id.btnClaimFullHouse)

        val gridTicket = findViewById<GridLayout>(R.id.gridTicket)
        val tvTicketPlaceholder = findViewById<TextView>(R.id.tvTicketPlaceholder)

        // Generate & display strict Tambola ticket
        currentTicket = generateTicket()
        displayTicket(currentTicket, gridTicket)

        tvTicketPlaceholder.visibility = View.GONE

        // Listen for game updates from the Host
        listenForGameUpdates()

        // Setup Claim Click Listeners
        btnClaimEarlyFive.setOnClickListener { validateClaim("Early Five", 10) { checkEarlyFive() } }
        btnClaimFourCorners.setOnClickListener { validateClaim("Four Corners", 15) { checkFourCorners() } }
        btnClaimTopLine.setOnClickListener { validateClaim("Top Line", 20) { checkLine(0) } }
        btnClaimMiddleLine.setOnClickListener { validateClaim("Middle Line", 20) { checkLine(1) } }
        btnClaimBottomLine.setOnClickListener { validateClaim("Bottom Line", 20) { checkLine(2) } }
        btnClaimFullHouse.setOnClickListener { validateClaim("Full House", 50) { checkFullHouse() } }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putIntegerArrayList("MARKED_NUMBERS", ArrayList(markedNumbers))
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

            // Listen for all called numbers
            database.child("rooms").child(code).child("calledNumbers")
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        calledNumbers.clear()
                        for (child in snapshot.children) {
                            val number = child.getValue(Int::class.java)
                            if (number != null) {
                                calledNumbers.add(number)
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        // Handle error
                    }
                })

            // Listen for claims
            database.child("rooms").child(code).child("claims")
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        winnersList.clear()
                        for (child in snapshot.children) {
                            val claimType = child.key
                            val winnerId = child.value.toString()
                            if (claimType != null) {
                                winnersList[claimType] = winnerId
                                disableClaimButton(claimType, winnerId)
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })

            // Listen for game status
            database.child("rooms").child(code).child("status")
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        when (snapshot.getValue(String::class.java)) {
                            "finished" -> showWinnersDialog()
                            "reset" -> resetPlayerState()
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
        }
    }

    private fun resetPlayerState() {
        Toast.makeText(this, "The host has reset the game! New ticket, same room.", Toast.LENGTH_LONG).show()

        // Clear local data
        markedNumbers.clear()
        calledNumbers.clear()
        winnersList.clear()

        // Regenerate and display a new ticket
        currentTicket = generateTicket()
        val gridTicket = findViewById<GridLayout>(R.id.gridTicket)
        displayTicket(currentTicket, gridTicket)

        // Reset UI elements
        tvLastCalledNumber.text = "Last Called: None"
        tvPlayerCurrentNumber.text = ""
        resetClaimButtons()
    }

    private fun resetClaimButtons() {
        btnClaimEarlyFive.isEnabled = true
        btnClaimEarlyFive.text = "Early Five"
        btnClaimFourCorners.isEnabled = true
        btnClaimFourCorners.text = "Four Corners"
        btnClaimTopLine.isEnabled = true
        btnClaimTopLine.text = "Top Line"
        btnClaimMiddleLine.isEnabled = true
        btnClaimMiddleLine.text = "Middle Line"
        btnClaimBottomLine.isEnabled = true
        btnClaimBottomLine.text = "Bottom Line"
        btnClaimFullHouse.isEnabled = true
        btnClaimFullHouse.text = "Full House"
    }

    private fun disableClaimButton(claimType: String, winnerId: String) {
        val button = when(claimType) {
            "Early Five" -> btnClaimEarlyFive
            "Four Corners" -> btnClaimFourCorners
            "Top Line" -> btnClaimTopLine
            "Middle Line" -> btnClaimMiddleLine
            "Bottom Line" -> btnClaimBottomLine
            "Full House" -> btnClaimFullHouse
            else -> null
        }
        
        button?.let {
            it.isEnabled = false
            it.text = "$claimType\n($winnerId)"
        }
    }

    private fun showWinnersDialog() {
        val message = StringBuilder()
        if (winnersList.isEmpty()) {
            message.append("No winners this time!")
        } else {
            for ((claim, winner) in winnersList) {
                message.append("$claim: $winner\n")
            }
        }
        
        if (!isFinishing) {
            AlertDialog.Builder(this)
                .setTitle("Game Over - Winners")
                .setMessage(message.toString())
                .setPositiveButton("OK") { _, _ -> finish() }
                .setCancelable(false)
                .show()
        }
    }

    // ------------------------------------------------------------
    //  CLAIM VALIDATION LOGIC
    // ------------------------------------------------------------

    private fun validateClaim(claimName: String, points: Int, validationCheck: () -> Boolean) {
        if (validationCheck()) {
             // Success: In a real app, notify the server/host
             roomCode?.let { code ->
                 val claimRef = database.child("rooms").child(code).child("claims").child(claimName)
                 claimRef.runTransaction(object : Transaction.Handler {
                     override fun doTransaction(currentData: MutableData): Transaction.Result {
                         if (currentData.value == null) {
                             currentData.value = playerId ?: "Unknown"
                             return Transaction.success(currentData)
                         }
                         return Transaction.abort()
                     }

                     override fun onComplete(
                         error: DatabaseError?,
                         committed: Boolean,
                         currentData: DataSnapshot?
                     ) {
                         if (committed) {
                             Toast.makeText(this@PlayerActivity, "Valid $claimName! You win $points points.", Toast.LENGTH_LONG).show()
                         } else {
                             Toast.makeText(this@PlayerActivity, "Too late! $claimName already claimed.", Toast.LENGTH_SHORT).show()
                         }
                     }
                 })
             }
        } else {
            // Failure
            Toast.makeText(this, "Bogus Claim! $claimName conditions not met.", Toast.LENGTH_SHORT).show()
        }
    }

    // Early Five: Any 5 numbers on the ticket are marked
    private fun checkEarlyFive(): Boolean {
        var markedCount = 0
        for (row in 0 until 3) {
            for (col in 0 until 9) {
                val num = currentTicket[row][col]
                if (num != 0 && calledNumbers.contains(num)) {
                    markedCount++
                }
            }
        }
        return markedCount >= 5
    }

    // Four Corners: 1st and last number of top and bottom rows
    private fun checkFourCorners(): Boolean {
        val topRowNumbers = currentTicket[0].filter { it != 0 }
        val bottomRowNumbers = currentTicket[2].filter { it != 0 }

        if (topRowNumbers.isEmpty() || bottomRowNumbers.isEmpty()) return false

        val topLeft = topRowNumbers.first()
        val topRight = topRowNumbers.last()
        val bottomLeft = bottomRowNumbers.first()
        val bottomRight = bottomRowNumbers.last()

        return calledNumbers.contains(topLeft) &&
               calledNumbers.contains(topRight) &&
               calledNumbers.contains(bottomLeft) &&
               calledNumbers.contains(bottomRight)
    }

    // Check a specific line (Top=0, Middle=1, Bottom=2)
    private fun checkLine(rowIndex: Int): Boolean {
        for (col in 0 until 9) {
            val num = currentTicket[rowIndex][col]
            if (num != 0 && !calledNumbers.contains(num)) {
                return false // Found an uncalled number in this row
            }
        }
        return true
    }

    // Full House: All 15 numbers marked
    private fun checkFullHouse(): Boolean {
         for (row in 0 until 3) {
            for (col in 0 until 9) {
                val num = currentTicket[row][col]
                if (num != 0 && !calledNumbers.contains(num)) {
                    return false
                }
            }
        }
        return true
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
        grid.setBackgroundColor(Color.TRANSPARENT)

        for (row in 0 until 3) {
            for (col in 0 until 9) {

                val value = ticket[row][col]
                val tv = TextView(this)

                val params = GridLayout.LayoutParams().apply {
                    width = 0
                    height = GridLayout.LayoutParams.WRAP_CONTENT
                    columnSpec = GridLayout.spec(col, 1f)
                    rowSpec = GridLayout.spec(row)
                    setMargins(6, 6, 6, 6)
                }

                tv.layoutParams = params
                tv.gravity = Gravity.CENTER
                tv.setPadding(4, 28, 4, 28)
                tv.textSize = 20f
                tv.typeface = Typeface.DEFAULT_BOLD

                if (value != 0) {
                    tv.text = value.toString()
                    val isMarked = markedNumbers.contains(value)
                    styleNumberCell(tv, isMarked)

                    if (isMarked) {
                        tv.isClickable = false
                    } else {
                        tv.setOnClickListener {
                            if (calledNumbers.contains(value)) {
                                markedNumbers.add(value)
                                styleNumberCell(tv, true)
                                tv.isClickable = false // Lock after marking
                            } else {
                                Toast.makeText(this@PlayerActivity, "This number has not been called yet", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }

                } else {
                    tv.text = ""
                    styleEmptyCell(tv)
                }

                grid.addView(tv)
            }
        }
    }

    private fun styleNumberCell(tv: TextView, marked: Boolean) {
        val drawable = GradientDrawable()
        drawable.shape = GradientDrawable.RECTANGLE
        drawable.cornerRadius = 16f
        
        if (marked) {
            drawable.setColor(Color.parseColor("#4CAF50")) // Green
            drawable.setStroke(2, Color.parseColor("#388E3C"))
            tv.setTextColor(Color.WHITE)
        } else {
            drawable.setColor(Color.WHITE)
            drawable.setStroke(2, Color.parseColor("#BDBDBD")) // Grey
            tv.setTextColor(Color.BLACK)
        }
        
        tv.background = drawable
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tv.elevation = if (marked) 2f else 6f
        }
    }

    private fun styleEmptyCell(tv: TextView) {
        val drawable = GradientDrawable()
        drawable.shape = GradientDrawable.RECTANGLE
        drawable.cornerRadius = 16f
        drawable.setColor(Color.parseColor("#E0E0E0")) // Block color
        
        tv.background = drawable
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tv.elevation = 0f
        }
    }
}

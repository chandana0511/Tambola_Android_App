package com.example.tambola

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

class HostActivity : AppCompatActivity() {
    // A set to hold the numbers that have been called during the game.
    private val calledNumbersSet = mutableSetOf<Int>()
    // A list of numbers that are still available to be called (1-90).
    private var availableNumbers = (1..90).toMutableList()
    // A complete list of all numbers (1-90) for display purposes.
    private val allNumbersDisplay = (1..90).toList()

    // UI elements
    private lateinit var tvCurrentNumber: TextView
    private lateinit var btnCallNumber: Button
    private lateinit var btnEndGame: Button
    private lateinit var btnResetGame: Button
    private lateinit var rvNumbers: RecyclerView
    private lateinit var numbersAdapter: NumbersAdapter
    private lateinit var tvRoomCode: TextView

    // Firebase database reference and current room code.
    private lateinit var database: DatabaseReference
    private var roomCode: String? = null

    // A map to store the winners of different claim types.
    private val winnersList = mutableMapOf<String, String>()

    private lateinit var winnerAnimationManager: WinnerAnimationManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_host)

        // Initialize Firebase and get the room code from the intent.
        roomCode = intent.getStringExtra("ROOM_CODE")
        database = FirebaseDatabase.getInstance("https://tambola-app-2823c-default-rtdb.asia-southeast1.firebasedatabase.app").reference

        // Initialize UI components.
        tvCurrentNumber = findViewById(R.id.tvCurrentNumber)
        btnCallNumber = findViewById(R.id.btnCallNumber)
        btnEndGame = findViewById(R.id.btnEndGame)
        btnResetGame = findViewById(R.id.btnResetGame)
        rvNumbers = findViewById(R.id.rvNumbers)
        tvRoomCode = findViewById(R.id.tvRoomCode)

        roomCode?.let {
            tvRoomCode.text = " $it"
        }

        val rootView = findViewById<View>(android.R.id.content)
        val gameUiViews = listOf<View>(findViewById(R.id.tvHostTitle), findViewById(R.id.cardCurrentNumber), findViewById(R.id.btnCallNumber), findViewById(R.id.rvNumbers), findViewById(R.id.linearLayoutButtons))
        winnerAnimationManager = WinnerAnimationManager(rootView, gameUiViews)

        btnEndGame.isEnabled = false

        setupRecyclerView()
        listenForGameChanges()

        // Set click listeners for the host's action buttons.
        btnCallNumber.setOnClickListener { callNextNumber() }
        btnEndGame.setOnClickListener { endGame() }
        btnResetGame.setOnClickListener { resetGame() }
    }

    /**
     * Sets up listeners for real-time updates from Firebase for claims and called numbers.
     */
    private fun listenForGameChanges() {
        roomCode?.let { code ->
            val roomRef = database.child("rooms").child(code)

            // Listener for claim submissions from players.
            roomRef.child("claims").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    winnersList.clear()
                    snapshot.children.forEach { child ->
                        winnersList[child.key!!] = child.value.toString()
                    }
                    // If a "Full House" is claimed, enable the End Game button.
                    if (winnersList.containsKey("Full House")) {
                        btnEndGame.isEnabled = true
                        btnCallNumber.isEnabled = false
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@HostActivity, "Error listening for claims: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })

            // Listener for the list of called numbers.
            roomRef.child("calledNumbers").addValueEventListener(object: ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    calledNumbersSet.clear()
                    snapshot.children.mapNotNullTo(calledNumbersSet) { it.getValue(Int::class.java) }
                    availableNumbers = (1..90).filter { !calledNumbersSet.contains(it) }.toMutableList()
                    numbersAdapter.notifyDataSetChanged()

                    val lastNumber = calledNumbersSet.lastOrNull()
                    if (lastNumber != null && lastNumber != 0) {
                        tvCurrentNumber.text = lastNumber.toString()
                    } else {
                        tvCurrentNumber.text = "Start"
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@HostActivity, "Error listening for numbers: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    /**
     * Initializes the RecyclerView to display the Tambola number board.
     */
    private fun setupRecyclerView() {
        numbersAdapter = NumbersAdapter(allNumbersDisplay, calledNumbersSet)
        rvNumbers.layoutManager = GridLayoutManager(this, 10)
        rvNumbers.adapter = numbersAdapter
    }

    /**
     * Calls the next random number if available and updates Firebase.
     */
    private fun callNextNumber() {
        if (availableNumbers.isNotEmpty()) {
            val number = availableNumbers.random()
            roomCode?.let {
                val newCalledNumbers = calledNumbersSet.toMutableList()
                newCalledNumbers.add(number)
                // Atomically update the list of called numbers in Firebase.
                database.child("rooms").child(it).child("calledNumbers").setValue(newCalledNumbers)
            }
        } else {
            tvCurrentNumber.text = "Done"
            btnCallNumber.isEnabled = false
            btnEndGame.isEnabled = true
        }
    }

    /**
     * Resets the entire game state, clears all data, and generates new tickets for all players.
     */
    private fun resetGame() {
        roomCode?.let { code ->
            val roomRef = database.child("rooms").child(code)

            // First, get a list of all players currently in the room.
            roomRef.child("tickets").addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val playerIds = snapshot.children.mapNotNull { it.key }

                    // Perform the reset by clearing all game-related data.
                    val resetData = mapOf<String, Any?>(
                        "calledNumbers" to listOf(0),
                        "claims" to null,
                        "markedNumbers" to null,
                        "tickets" to null,
                        "status" to "waiting",
                        "resetVersion" to ServerValue.increment(1)
                    )
                    roomRef.updateChildren(resetData).addOnSuccessListener {
                        // After the reset is successful, generate new unique tickets for all players in the background.
                        lifecycleScope.launch {
                            regenerateTicketsForPlayers(code, playerIds)
                            Toast.makeText(this@HostActivity, "Game Reset! New tickets are being assigned.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@HostActivity, "Failed to get players for reset: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }

        btnCallNumber.isEnabled = true
        btnEndGame.isEnabled = false
    }

    /**
     * Generates and assigns new, unique tickets to a list of player IDs after a reset.
     * This is a suspend function to allow it to be called from a coroutine.
     * @param roomCode The code of the current room.
     * @param playerIds The list of player IDs to generate tickets for.
     */
    private suspend fun regenerateTicketsForPlayers(roomCode: String, playerIds: List<String>) = withContext(Dispatchers.IO) {
        val allGeneratedTickets = mutableSetOf<List<List<Int>>>()
        val ticketsToAssign = mutableMapOf<String, List<List<Int>>>()

        for (playerId in playerIds) {
            var ticket: List<List<Int>>
            // Keep generating tickets until a unique one is found.
            do {
                ticket = generateTicket()
            } while (allGeneratedTickets.contains(ticket))

            allGeneratedTickets.add(ticket)
            ticketsToAssign[playerId] = ticket
        }

        withContext(Dispatchers.Main) {
            for ((playerId, ticket) in ticketsToAssign) {
                // Assign the unique ticket to the player and initialize their marked numbers.
                database.child("rooms").child(roomCode).child("tickets").child(playerId).setValue(ticket)
                database.child("rooms").child(roomCode).child("markedNumbers").child(playerId).setValue(listOf(0))
            }
        }
    }


    /**
     * Generates a single, valid Tambola ticket.
     * Guarantees 3 rows, 9 columns, 5 numbers per row, and at least one number per column.
     * @return A 2D list representing the ticket.
     */
    private fun generateTicket(): List<List<Int>> {
        var ticket: Array<Array<Int>>
        var isValid: Boolean
        do {
            ticket = Array(3) { Array(9) { 0 } } // 3x9 grid, 0 represents an empty cell
            val usedNumbers = mutableSetOf<Int>() // Ensure no number is used more than once on a single ticket
            isValid = true

            // Rule: Each row must have exactly 5 numbers.
            for (row in 0 until 3) {
                val columnsToFill = (0 until 9).shuffled().take(5)
                for (col in columnsToFill) {
                    val min = col * 10 + 1
                    val max = if (col == 8) 90 else col * 10 + 10
                    val possibleNumbers = (min..max).filter { !usedNumbers.contains(it) }

                    if (possibleNumbers.isEmpty()) {
                        isValid = false // Generation failed, retry
                        break
                    }
                    val number = possibleNumbers.random()
                    ticket[row][col] = number
                    usedNumbers.add(number)
                }
                if (!isValid) break
            }

            if (isValid) {
                // Rule: Each column must have at least one number.
                for (col in 0 until 9) {
                    if (ticket.all { row -> row[col] == 0 }) {
                        isValid = false // Generation failed, retry
                        break
                    }
                }
            }
        } while (!isValid)

        // Rule: Numbers in each column must be sorted vertically.
        for (col in 0..8) {
            val colValues = ticket.map { it[col] }.filter { it != 0 }.sorted()
            var valueIndex = 0
            for (row in 0..2) {
                if (ticket[row][col] != 0) {
                    ticket[row][col] = colValues[valueIndex++]
                }
            }
        }

        return ticket.map { it.toList() }
    }


    /**
     * Ends the game, sets the status to "finished", and shows the winner animation.
     */
    private fun endGame() {
        roomCode?.let { database.child("rooms").child(it).child("status").setValue("finished") }
        winnerAnimationManager.startWinnerSequence(winnersList) { finish() }
    }
}

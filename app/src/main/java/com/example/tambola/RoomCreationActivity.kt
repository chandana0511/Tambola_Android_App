package com.example.tambola

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlin.random.Random

class RoomCreationActivity : AppCompatActivity() {

    private lateinit var btnCreateRoom: Button
    private lateinit var layoutRoomDetails: LinearLayout
    private lateinit var tvRoomCode: TextView
    private lateinit var btnShare: Button
    private lateinit var tvPlayerCount: TextView
    private lateinit var btnStartGame: Button

    private lateinit var database: DatabaseReference
    private var currentRoomCode: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_room_creation)

        try {
            database = FirebaseDatabase.getInstance("https://tambola-app-2823c-default-rtdb.asia-southeast1.firebasedatabase.app").reference
        } catch (e: Exception) {
            Log.e("FirebaseInit", "Error initializing database: ${e.message}")
            Toast.makeText(this, "Database Error: ${e.message}", Toast.LENGTH_LONG).show()
        }

        btnCreateRoom = findViewById(R.id.btnCreateRoom)
        layoutRoomDetails = findViewById(R.id.layoutRoomDetails)
        tvRoomCode = findViewById(R.id.tvRoomCode)
        btnShare = findViewById(R.id.btnShare)
        tvPlayerCount = findViewById(R.id.tvPlayerCount)
        btnStartGame = findViewById(R.id.btnStartGame)

        setupProfileIcon()

        btnCreateRoom.setOnClickListener { createRoom() }
        btnShare.setOnClickListener { shareRoomCode() }

        btnStartGame.setOnClickListener {
            currentRoomCode?.let { database.child("rooms").child(it).child("status").setValue("running") }
            val intent = Intent(this, HostActivity::class.java)
            intent.putExtra("ROOM_CODE", currentRoomCode)
            startActivity(intent)
            finish()
        }
        cleanupOldRooms()
    }

    private fun cleanupOldRooms() {
        val roomsRef = database.child("rooms")
        val twentyFourHoursAgo = System.currentTimeMillis() - 24 * 60 * 60 * 1000
        roomsRef.orderByChild("timestamp").endAt(twentyFourHoursAgo.toDouble())
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (roomSnapshot in snapshot.children) {
                        roomSnapshot.ref.removeValue()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("FirebaseCleanup", "Failed to clean up old rooms: ${error.message}")
                }
            })
    }


    private fun setupProfileIcon() {
        val ivProfile = findViewById<ImageView>(R.id.ivProfile)
        ivProfile.setOnClickListener { view -> showProfileMenu(view) }
    }

    private fun showProfileMenu(view: View) {
        val popup = PopupMenu(this, view)
        val currentUser = FirebaseAuth.getInstance().currentUser
        val email = currentUser?.email ?: "User"

        popup.menu.add(0, 0, 0, email).apply { isEnabled = false }
        popup.menu.add(0, 1, 1, "Logout")

        popup.setOnMenuItemClickListener { item ->
            if (item.itemId == 1) {
                logout()
                true
            } else false
        }
        popup.show()
    }

    private fun logout() {
        FirebaseAuth.getInstance().signOut()
        val intent = Intent(this, AuthActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun createRoom() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val roomCode = Random.nextInt(100000, 999999).toString()
        currentRoomCode = roomCode

        val roomData = mapOf(
            "status" to "waiting",
            "hostId" to userId,
            "resetVersion" to 1,
            "calledNumbers" to listOf(0),
            "tickets" to emptyMap<String, Any>(),
            "markedNumbers" to emptyMap<String, Any>(),
            "claims" to emptyMap<String, Any>(),
            "timestamp" to System.currentTimeMillis()
        )

        database.child("rooms").child(roomCode).setValue(roomData).addOnSuccessListener {
            tvRoomCode.text = roomCode
            btnCreateRoom.visibility = View.GONE
            layoutRoomDetails.visibility = View.VISIBLE
            listenForPlayers(roomCode)
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Failed to create room: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun listenForPlayers(roomCode: String) {
        database.child("rooms").child(roomCode).child("tickets").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val count = snapshot.childrenCount
                tvPlayerCount.text = "Players Joined: $count"

                snapshot.children.forEach { playerSnapshot ->
                    val playerId = playerSnapshot.key
                    val needsTicket = playerSnapshot.getValue(true) is Boolean

                    if (playerId != null && needsTicket) {
                        generateAndAssignTicketForPlayer(roomCode, playerId)
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseCheck", "Listener Cancelled: ${error.message}")
            }
        })
    }

    private fun generateAndAssignTicketForPlayer(roomCode: String, playerId: String) {
        val ticket = generateTicket()
        database.child("rooms").child(roomCode).child("tickets").child(playerId).setValue(ticket)
        database.child("rooms").child(roomCode).child("markedNumbers").child(playerId).setValue(listOf(0))
    }

    private fun generateTicket(): List<List<Int>> {
        var ticket: Array<Array<Int>>
        var isValid: Boolean
        do {
            ticket = Array(3) { Array(9) { 0 } }
            val usedNumbers = mutableSetOf<Int>()
            isValid = true

            for (row in 0 until 3) {
                val columns = (0 until 9).shuffled().take(5)
                for (col in columns) {
                    val min = col * 10 + 1
                    val max = if (col == 8) 90 else col * 10 + 10
                    val possibleNumbers = (min..max).filter { !usedNumbers.contains(it) }
                    if (possibleNumbers.isEmpty()) {
                        isValid = false
                        break
                    }
                    val number = possibleNumbers.random()
                    ticket[row][col] = number
                    usedNumbers.add(number)
                }
                if (!isValid) break
            }

            if (isValid) {
                for (col in 0 until 9) {
                    if (ticket.all { row -> row[col] == 0 }) {
                        isValid = false
                        break
                    }
                }
            }
        } while (!isValid)

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

    private fun shareRoomCode() {
        val code = tvRoomCode.text.toString()
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "Join my Tambola game! Room Code: $code")
        }
        startActivity(Intent.createChooser(shareIntent, "Share Room Code"))
    }
}
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

        // 1. Initialize Firebase with the explicit Asia-Southeast URL
        // We use .reference to get the root DatabaseReference
        try {
            database = FirebaseDatabase.getInstance("https://tambola-app-2823c-default-rtdb.asia-southeast1.firebasedatabase.app").reference
        } catch (e: Exception) {
            Log.e("FirebaseInit", "Error initializing database: ${e.message}")
            Toast.makeText(this, "Database Error: ${e.message}", Toast.LENGTH_LONG).show()
        }

        // 2. Initialize Views
        btnCreateRoom = findViewById(R.id.btnCreateRoom)
        layoutRoomDetails = findViewById(R.id.layoutRoomDetails)
        tvRoomCode = findViewById(R.id.tvRoomCode)
        btnShare = findViewById(R.id.btnShare)
        tvPlayerCount = findViewById(R.id.tvPlayerCount)
        btnStartGame = findViewById(R.id.btnStartGame)

        setupProfileIcon()

        btnCreateRoom.setOnClickListener {
            createRoom()
        }

        btnShare.setOnClickListener {
            shareRoomCode()
        }

        btnStartGame.setOnClickListener {
            val intent = Intent(this, HostActivity::class.java)
            intent.putExtra("ROOM_CODE", currentRoomCode)
            startActivity(intent)
            finish()
        }
    }

    private fun setupProfileIcon() {
        val ivProfile = findViewById<ImageView>(R.id.ivProfile)
        ivProfile.setOnClickListener { view ->
            showProfileMenu(view)
        }
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
            } else {
                false
            }
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
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val roomCode = Random.nextInt(100000, 999999).toString()
        currentRoomCode = roomCode

        // Prepare data
        val roomData = mapOf(
            "status" to "waiting",
            "hostId" to userId,
            "timestamp" to System.currentTimeMillis()
        )

        Log.d("FirebaseCheck", "Attempting to write to: rooms/$roomCode")

        // 3. Write to Database
        database.child("rooms").child(roomCode).setValue(roomData)
            .addOnSuccessListener {
                // SUCCESS LOGIC
                Log.d("FirebaseCheck", "SUCCESS! Room created at rooms/$roomCode")

                tvRoomCode.text = roomCode
                btnCreateRoom.visibility = View.GONE
                layoutRoomDetails.visibility = View.VISIBLE

                listenForPlayers(roomCode)
            }
            .addOnFailureListener { e ->
                // FAILURE LOGIC
                Log.e("FirebaseCheck", "FAILURE: ${e.message}")
                Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun listenForPlayers(roomCode: String) {
        database.child("rooms").child(roomCode).child("players")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val count = snapshot.childrenCount
                    tvPlayerCount.text = "Players Joined: $count"
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("FirebaseCheck", "Listener Cancelled: ${error.message}")
                }
            })
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

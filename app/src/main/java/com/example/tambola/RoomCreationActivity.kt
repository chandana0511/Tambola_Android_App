package com.example.tambola

import android.content.Intent
import android.os.Bundle
//import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
//import com.google.firebase.FirebaseApp
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

//    companion object {
//        private const val TAG = "RoomCreationActivity"
//    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_room_creation)

        // Initialize Firebase
        database = FirebaseDatabase.getInstance().reference

        // Log Firebase Configuration
//        try {
//            val options = FirebaseApp.getInstance().options
//            Log.d(TAG, "Firebase Config - Project ID: ${options.projectId}")
//            Log.d(TAG, "Firebase Config - Database URL: ${options.databaseUrl}")
//            Log.d(TAG, "Firebase Config - Application ID: ${options.applicationId}")
//        } catch (e: Exception) {
//            Log.e(TAG, "Error reading Firebase options", e)
//        }

        btnCreateRoom = findViewById(R.id.btnCreateRoom)
        layoutRoomDetails = findViewById(R.id.layoutRoomDetails)
        tvRoomCode = findViewById(R.id.tvRoomCode)
        btnShare = findViewById(R.id.btnShare)
        tvPlayerCount = findViewById(R.id.tvPlayerCount)
        btnStartGame = findViewById(R.id.btnStartGame)

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

    private fun createRoom() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val roomCode = Random.nextInt(100000, 999999).toString()
        currentRoomCode = roomCode
        
        // Save room to Firebase
        val roomData = mapOf(
            "status" to "waiting", // waiting, active, finished
            "hostId" to userId,
            "currentNumber" to 0
            // "players" will be a sub-node added as they join
        )

        //Log.d(TAG, "Creating room with code: $roomCode")
        
        database.child("rooms").child(roomCode).setValue(roomData)
            .addOnSuccessListener {
                //Log.d(TAG, "Room created successfully in Firebase")
                tvRoomCode.text = roomCode
                
                // Update UI visibility
                btnCreateRoom.visibility = View.GONE
                layoutRoomDetails.visibility = View.VISIBLE
                
                // Listen for player joins
                listenForPlayers(roomCode)
            }
            .addOnFailureListener { e ->
                //Log.e(TAG, "Failed to create room", e)
                Toast.makeText(this, "Failed to create room: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun listenForPlayers(roomCode: String) {
       // Log.d(TAG, "Listening for players in room: $roomCode")
        database.child("rooms").child(roomCode).child("players")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val count = snapshot.childrenCount
                  //  Log.d(TAG, "Players updated. Count: $count")
                    tvPlayerCount.text = "Players Joined: $count"
                }

                override fun onCancelled(error: DatabaseError) {
                   // Log.e(TAG, "Database error while listening for players", error.toException())
                    Toast.makeText(this@RoomCreationActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun shareRoomCode() {
        val roomCode = tvRoomCode.text.toString()
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "Join my Tambola game! Room Code: $roomCode")
        }
        startActivity(Intent.createChooser(shareIntent, "Share Room Code"))
    }
}

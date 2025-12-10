package com.example.tambola

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlin.random.Random

class RoomCreationActivity : AppCompatActivity() {

    private lateinit var btnCreateRoom: Button
    private lateinit var layoutRoomDetails: LinearLayout
    private lateinit var tvRoomCode: TextView
    private lateinit var btnShare: Button
    private lateinit var tvPlayerCount: TextView
    private lateinit var btnStartGame: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_room_creation)

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
            startActivity(intent)
            finish() // Optional: Finish this activity so back button doesn't come back here
        }
    }

    private fun createRoom() {
        // Generate a random 6-digit code
        val roomCode = Random.nextInt(100000, 999999).toString()
        
        tvRoomCode.text = roomCode
        tvPlayerCount.text = "Players Joined: 0" // Initial count

        // Update UI visibility
        btnCreateRoom.visibility = View.GONE
        layoutRoomDetails.visibility = View.VISIBLE
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

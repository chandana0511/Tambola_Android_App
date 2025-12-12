package com.example.tambola

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class RoomJoinActivity : AppCompatActivity() {

    private val editTexts =  arrayOfNulls<EditText>(6)
    private lateinit var database: DatabaseReference
    private lateinit var tvError: TextView

//    companion object {
//        private const val TAG = "RoomJoinActivity"
//    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_room_join)

        database = FirebaseDatabase.getInstance().reference

        editTexts[0] = findViewById(R.id.etDigit1)
        editTexts[1] = findViewById(R.id.etDigit2)
        editTexts[2] = findViewById(R.id.etDigit3)
        editTexts[3] = findViewById(R.id.etDigit4)
        editTexts[4] = findViewById(R.id.etDigit5)
        editTexts[5] = findViewById(R.id.etDigit6)

        val btnJoinGame = findViewById<Button>(R.id.btnJoinGame)
        tvError = findViewById(R.id.tvError)

        setupOtpInputs()

        btnJoinGame.setOnClickListener {
            val code = getEnteredCode()
            //Log.d(TAG, "Attempting to join room with code: $code")
            if (code.length == 6) {
                validateAndJoinRoom(code)
            } else {
                //Log.w(TAG, "Incomplete code entered: $code")
                tvError.text = "Please enter a complete 6-digit code"
                tvError.visibility = View.VISIBLE
            }
        }
    }

    private fun validateAndJoinRoom(code: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            //Log.e(TAG, "User not authenticated")
            Toast.makeText(this, "User not authenticated. Please log in.", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, AuthActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            return
        }

        //Log.d(TAG, "Checking if room $code exists...")
        // Use get() to fetch the data once and check for existence
        database.child("rooms").child(code).get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                //Log.d(TAG, "Room $code exists. Joining...")
                // Room exists, join it
                joinRoom(code, userId)
            } else {
                //Log.w(TAG, "Room $code does not exist")
                tvError.text = "Invalid Room Code. Please check and try again."
                tvError.visibility = View.VISIBLE
            }
        }.addOnFailureListener {
            //Log.e(TAG, "Network error checking room existence", it)
            tvError.text = "Network Error. Please try again."
            tvError.visibility = View.VISIBLE
            Toast.makeText(this@RoomJoinActivity, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun joinRoom(code: String, userId: String) {
        //Log.d(TAG, "Adding player $userId to room $code")
        // Use the Authenticated User ID as the Player ID
        database.child("rooms").child(code).child("players").child(userId).setValue(true)
            .addOnSuccessListener {
                //Log.d(TAG, "Successfully joined room $code")
                tvError.visibility = View.GONE
                val intent = Intent(this@RoomJoinActivity, PlayerActivity::class.java)
                intent.putExtra("ROOM_CODE", code)
                intent.putExtra("PLAYER_ID", userId)
                startActivity(intent)
                finish()
            }
            .addOnFailureListener {
                //Log.e(TAG, "Failed to write player data to Firebase", it)
                tvError.text = "Failed to join room."
                tvError.visibility = View.VISIBLE
                Toast.makeText(this@RoomJoinActivity, "Failed to join: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupOtpInputs() {
        for (i in 0 until 6) {
            editTexts[i]?.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    if (s?.length == 1 && i < 5) {
                        editTexts[i + 1]?.requestFocus()
                    }
                }
                override fun afterTextChanged(s: Editable?) {}
            })

            editTexts[i]?.setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_DEL && event.action == KeyEvent.ACTION_DOWN) {
                    if (editTexts[i]?.text.isNullOrEmpty() && i > 0) {
                        editTexts[i - 1]?.requestFocus()
                        editTexts[i - 1]?.setText("") // Clear previous digit too
                        return@setOnKeyListener true
                    }
                }
                false
            }
        }
    }

    private fun getEnteredCode(): String {
        val sb = StringBuilder()
        for (et in editTexts) {
            sb.append(et?.text.toString())
        }
        return sb.toString()
    }
}

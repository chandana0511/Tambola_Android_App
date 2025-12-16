package com.example.tambola

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class RoomJoinActivity : AppCompatActivity() {

    private val editTexts =  arrayOfNulls<EditText>(6)
    private lateinit var database: DatabaseReference
    private lateinit var tvError: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_room_join)

        //database = FirebaseDatabase.getInstance().reference
        // Explicitly tell the app to look at your Asia Southeast server
        database = FirebaseDatabase.getInstance("https://tambola-app-2823c-default-rtdb.asia-southeast1.firebasedatabase.app").reference
        
        editTexts[0] = findViewById(R.id.etDigit1)
        editTexts[1] = findViewById(R.id.etDigit2)
        editTexts[2] = findViewById(R.id.etDigit3)
        editTexts[3] = findViewById(R.id.etDigit4)
        editTexts[4] = findViewById(R.id.etDigit5)
        editTexts[5] = findViewById(R.id.etDigit6)

        val btnJoinGame = findViewById<Button>(R.id.btnJoinGame)
        tvError = findViewById(R.id.tvError)

        setupProfileIcon()
        setupOtpInputs()

        btnJoinGame.setOnClickListener {
            val code = getEnteredCode()
            if (code.length == 6) {
                validateAndJoinRoom(code)
            } else {
                tvError.text = "Please enter a complete 6-digit code"
                tvError.visibility = View.VISIBLE
            }
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

        // Add user email as a disabled item (header)
        popup.menu.add(0, 0, 0, email).apply {
            isEnabled = false
        }

        // Add Logout option
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

    private fun validateAndJoinRoom(code: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val userId = currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "User not authenticated. Please log in.", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, AuthActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            return
        }

        // Extract prefix from email
        val email = currentUser?.email
        val displayName = if (!email.isNullOrEmpty() && email.contains("@")) {
            email.substringBefore("@")
        } else {
            userId 
        }

        database.child("rooms").child(code).get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                joinRoom(code, userId, displayName)
            } else {
                tvError.text = "Invalid Room Code. Please check and try again."
                tvError.visibility = View.VISIBLE
            }
        }.addOnFailureListener {
            tvError.text = "Network Error. Please try again."
            tvError.visibility = View.VISIBLE
            Toast.makeText(this@RoomJoinActivity, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun joinRoom(code: String, userId: String, displayName: String) {
        database.child("rooms").child(code).child("players").child(userId).setValue(true)
            .addOnSuccessListener {
                tvError.visibility = View.GONE
                val intent = Intent(this@RoomJoinActivity, PlayerActivity::class.java)
                intent.putExtra("ROOM_CODE", code)
                intent.putExtra("PLAYER_ID", displayName) // Pass display name instead of raw UID
                startActivity(intent)
                finish()
            }
            .addOnFailureListener {
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

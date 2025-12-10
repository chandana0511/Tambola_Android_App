package com.example.tambola

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class RoomJoinActivity : AppCompatActivity() {

    private val editTexts =  arrayOfNulls<EditText>(6)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_room_join)

        editTexts[0] = findViewById(R.id.etDigit1)
        editTexts[1] = findViewById(R.id.etDigit2)
        editTexts[2] = findViewById(R.id.etDigit3)
        editTexts[3] = findViewById(R.id.etDigit4)
        editTexts[4] = findViewById(R.id.etDigit5)
        editTexts[5] = findViewById(R.id.etDigit6)

        val btnJoinGame = findViewById<Button>(R.id.btnJoinGame)
        val tvError = findViewById<TextView>(R.id.tvError)

        setupOtpInputs()

        btnJoinGame.setOnClickListener {
            val code = getEnteredCode()
            if (code.length == 6) {
                // TODO: Validate code with Host (SharedPref/Firebase/Network)
                // For now, any 6 digit code allows entry
                tvError.visibility = View.GONE
                val intent = Intent(this, PlayerActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                tvError.text = "Please enter a complete 6-digit code"
                tvError.visibility = View.VISIBLE
            }
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

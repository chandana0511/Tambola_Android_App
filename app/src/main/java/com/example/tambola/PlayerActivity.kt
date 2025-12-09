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
import kotlin.random.Random

class PlayerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        val btnClaimLine1 = findViewById<MaterialButton>(R.id.btnClaimLine1)
        val btnClaimFullHouse = findViewById<MaterialButton>(R.id.btnClaimFullHouse)
        val gridTicket = findViewById<GridLayout>(R.id.gridTicket)
        val tvTicketPlaceholder = findViewById<TextView>(R.id.tvTicketPlaceholder)

        // Generate and display ticket
        val ticket = generateTicket()
        displayTicket(ticket, gridTicket)
        
        // Hide placeholder
        tvTicketPlaceholder.visibility = View.GONE

        btnClaimLine1.setOnClickListener {
            Toast.makeText(this, "Top Line Claimed! (Verification needed)", Toast.LENGTH_SHORT).show()
        }

        btnClaimFullHouse.setOnClickListener {
            Toast.makeText(this, "Full House Claimed! (Verification needed)", Toast.LENGTH_SHORT).show()
        }
    }

    private fun generateTicket(): Array<IntArray> {
        // 3 rows, 9 columns
        val ticket = Array(3) { IntArray(9) { 0 } }
        
        // Rule: Each row has exactly 5 numbers
        for (row in 0 until 3) {
            // Pick 5 unique columns for this row
            val indices = (0 until 9).toMutableList()
            indices.shuffle()
            val selectedCols = indices.take(5).sorted()
            
            for (col in selectedCols) {
                var num: Int
                var attempts = 0
                do {
                   val start = if (col == 0) 1 else col * 10
                   val end = if (col == 8) 90 else (col * 10) + 9
                   num = Random.nextInt(start, end + 1)
                   attempts++
                } while (isNumberInCol(ticket, col, num) && attempts < 100)
                
                ticket[row][col] = num
            }
        }
        
        // Sort columns vertically (standard Tambola rule)
        for (col in 0 until 9) {
            val colNums = mutableListOf<Int>()
            for (row in 0 until 3) {
                if (ticket[row][col] != 0) colNums.add(ticket[row][col])
            }
            colNums.sort()
            
            var idx = 0
            for (row in 0 until 3) {
                if (ticket[row][col] != 0) {
                    ticket[row][col] = colNums[idx++]
                }
            }
        }

        return ticket
    }

    private fun isNumberInCol(ticket: Array<IntArray>, col: Int, num: Int): Boolean {
        for (r in 0 until 3) {
            if (ticket[r][col] == num) return true
        }
        return false
    }

    private fun displayTicket(ticket: Array<IntArray>, grid: GridLayout) {
        grid.removeAllViews()
        // Iterate rows and cols
        for (row in 0 until 3) {
            for (col in 0 until 9) {
                val value = ticket[row][col]
                val tv = TextView(this)
                
                val params = GridLayout.LayoutParams()
                // Use column weight to distribute width evenly
                params.width = 0
                params.height = GridLayout.LayoutParams.WRAP_CONTENT
                params.columnSpec = GridLayout.spec(col, 1f)
                params.rowSpec = GridLayout.spec(row)
                params.setMargins(4, 4, 4, 4)
                
                tv.layoutParams = params
                tv.gravity = Gravity.CENTER
                tv.setPadding(8, 32, 8, 32)
                tv.textSize = 18f
                
                if (value != 0) {
                    tv.text = value.toString()
                    tv.setBackgroundColor(Color.parseColor("#E2E8F0")) // Light gray
                    tv.setTextColor(Color.BLACK)
                    
                    // Click listener to mark numbers
                    tv.setOnClickListener {
                        if (tv.tag == "marked") {
                             tv.setBackgroundColor(Color.parseColor("#E2E8F0"))
                             tv.setTextColor(Color.BLACK)
                             tv.tag = null
                        } else {
                             tv.setBackgroundColor(Color.parseColor("#68D391")) // Green
                             tv.setTextColor(Color.WHITE)
                             tv.tag = "marked"
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

package com.example.tambola

import android.animation.Animator
import android.content.res.ColorStateList
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
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.airbnb.lottie.LottieAnimationView
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction

class PlayerActivity : AppCompatActivity() {

    private val viewModel: PlayerViewModel by viewModels()
    private var roomCode: String? = null
    private var playerId: String? = null
    private var displayName: String? = null
    private lateinit var database: DatabaseReference

    // UI Elements
    private lateinit var tvLastCalledNumber: TextView
    private lateinit var tvPlayerCurrentNumber: TextView
    private lateinit var gridTicket: GridLayout
    private lateinit var tvTicketPlaceholder: TextView
    private lateinit var confettiAnimation: LottieAnimationView
    private lateinit var winnerAnimationManager: WinnerAnimationManager

    // Game State
    private var markedNumbers = setOf<Int>()
    private var calledNumbers = setOf<Int>()
    private var currentTicket: Array<IntArray>? = null
    private var winnersList = mapOf<String, String>()
    private var currentResetVersion: Long = 0

    // Claim Buttons
    private lateinit var btnClaimEarlyFive: Button
    private lateinit var btnClaimFourCorners: Button
    private lateinit var btnClaimTopLine: Button
    private lateinit var btnClaimMiddleLine: Button
    private lateinit var btnClaimBottomLine: Button
    private lateinit var btnClaimFullHouse: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        roomCode = intent.getStringExtra("ROOM_CODE")
        playerId = intent.getStringExtra("PLAYER_ID")
        displayName = intent.getStringExtra("DISPLAY_NAME")
        database = FirebaseDatabase.getInstance("https://tambola-app-2823c-default-rtdb.asia-southeast1.firebasedatabase.app").reference


        initializeViews()

        setupClaimButtonListeners()

        if (roomCode != null && playerId != null) {
            viewModel.init(roomCode!!, playerId!!)
            observeViewModel()
        } else {
            Toast.makeText(this, "Error: Room code or Player ID is missing.", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun initializeViews() {
        tvLastCalledNumber = findViewById(R.id.tvLastCalledNumber)
        tvPlayerCurrentNumber = findViewById(R.id.tvPlayerCurrentNumber)
        gridTicket = findViewById(R.id.gridTicket)
        tvTicketPlaceholder = findViewById(R.id.tvTicketPlaceholder)
        confettiAnimation = findViewById(R.id.confetti_animation)

        btnClaimEarlyFive = findViewById(R.id.btnClaimEarlyFive)
        btnClaimFourCorners = findViewById(R.id.btnClaimFourCorners)
        btnClaimTopLine = findViewById(R.id.btnClaimTopLine)
        btnClaimMiddleLine = findViewById(R.id.btnClaimMiddleLine)
        btnClaimBottomLine = findViewById(R.id.btnClaimBottomLine)
        btnClaimFullHouse = findViewById(R.id.btnClaimFullHouse)

        val rootView = findViewById<View>(android.R.id.content)
        val gameUiViews = listOf<View>(findViewById(R.id.tvPlayerTitle), findViewById(R.id.cardPlayerCurrentNumber), findViewById(R.id.tvLastCalledNumber), findViewById(R.id.cardTicket), findViewById(R.id.btnClaimEarlyFive), findViewById(R.id.btnClaimFourCorners), findViewById(R.id.btnClaimTopLine), findViewById(R.id.btnClaimMiddleLine), findViewById(R.id.btnClaimBottomLine), findViewById(R.id.btnClaimFullHouse))
        winnerAnimationManager = WinnerAnimationManager(rootView, gameUiViews)

        resetClaimButtonsUI()
    }

    private fun observeViewModel() {
        viewModel.ticket.observe(this, Observer { ticket ->
            if (ticket != null) {
                currentTicket = ticket
                tvTicketPlaceholder.visibility = View.GONE
                gridTicket.visibility = View.VISIBLE
                displayTicket()
            } else {
                tvTicketPlaceholder.text = "Waiting for host to assign ticket..."
                tvTicketPlaceholder.visibility = View.VISIBLE
                gridTicket.visibility = View.GONE
            }
        })

        viewModel.markedNumbers.observe(this, Observer { numbers ->
            markedNumbers = numbers
            if (currentTicket != null) displayTicket()
        })

        viewModel.calledNumbers.observe(this, Observer { numbers ->
            calledNumbers = numbers
            updateCalledNumberUI()
            if (currentTicket != null) displayTicket()
        })

        viewModel.winners.observe(this, Observer { winners ->
            winnersList = winners
            resetClaimButtonsUI()
            winners.forEach { (claimType, winnerName) ->
                disableClaimButton(claimType, winnerName)
            }
        })

        viewModel.gameStatus.observe(this, Observer { status ->
            if (status == "finished") {
                winnerAnimationManager.startWinnerSequence(winnersList) { finish() }
            }
        })

        viewModel.resetVersion.observe(this, Observer { newVersion ->
            if (currentResetVersion == 0L) {
                currentResetVersion = newVersion
            } else if (newVersion > currentResetVersion) {
                currentResetVersion = newVersion
                resetPlayerState()
            }
        })

        viewModel.error.observe(this, Observer { errorMessage ->
            Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
        })
    }

    private fun setupClaimButtonListeners() {
        btnClaimEarlyFive.setOnClickListener { validateClaim("Early Five") { checkEarlyFive() } }
        btnClaimFourCorners.setOnClickListener { validateClaim("Four Corners") { checkFourCorners() } }
        btnClaimTopLine.setOnClickListener { validateClaim("Top Line") { checkLine(0) } }
        btnClaimMiddleLine.setOnClickListener { validateClaim("Middle Line") { checkLine(1) } }
        btnClaimBottomLine.setOnClickListener { validateClaim("Bottom Line") { checkLine(2) } }
        btnClaimFullHouse.setOnClickListener { validateClaim("Full House") { checkFullHouse() } }
    }

    private fun resetPlayerState() {
        Toast.makeText(this, "The host has reset the game!", Toast.LENGTH_SHORT).show()
        updateCalledNumberUI()
        resetClaimButtonsUI()
        if (currentTicket != null) {
            displayTicket()
        }
    }

    private fun resetClaimButtonsUI() {
        val buttons = listOf(btnClaimEarlyFive, btnClaimFourCorners, btnClaimTopLine, btnClaimMiddleLine, btnClaimBottomLine, btnClaimFullHouse)
        val claimTypes = listOf("Early Five", "Four Corners", "Top Line", "Middle Line", "Bottom Line", "Full House")

        buttons.forEachIndexed { index, button ->
            button.isEnabled = true
            button.text = claimTypes[index]
            button.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#1D3CC7"))
            button.setTextColor(Color.WHITE)
        }
    }

    private fun updateCalledNumberUI() {
        val lastNumber = calledNumbers.lastOrNull()
        if (lastNumber != null && lastNumber != 0) {
            tvLastCalledNumber.text = "Last Called: $lastNumber"
            tvPlayerCurrentNumber.text = lastNumber.toString()
        } else {
            tvLastCalledNumber.text = "Last Called: -"
            tvPlayerCurrentNumber.text = "-"
        }
    }

    private fun disableClaimButton(claimType: String, winnerId: String) {
        val button = when (claimType) {
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
            it.text = "$claimType($winnerId)"
            it.setTextColor(Color.WHITE)
            it.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#4CAF50"))
        }
    }

    private fun validateClaim(claimName: String, validationCheck: () -> Boolean) {
        if (currentTicket == null) {
             Toast.makeText(this, "Ticket not loaded yet.", Toast.LENGTH_SHORT).show()
             return
        }
        if (validationCheck()) {
             roomCode?.let { code ->
                 val claimRef = database.child("rooms").child(code).child("claims").child(claimName)
                 claimRef.runTransaction(object : Transaction.Handler {
                     override fun doTransaction(currentData: MutableData): Transaction.Result {
                         return if (currentData.value == null) {
                             currentData.value = displayName ?: playerId ?: "Unknown"
                             Transaction.success(currentData)
                         } else Transaction.abort()
                     }
                     override fun onComplete(error: com.google.firebase.database.DatabaseError?, committed: Boolean, currentData: com.google.firebase.database.DataSnapshot?) {
                         if (committed) {
                            Toast.makeText(this@PlayerActivity, "Valid $claimName! Claim submitted.", Toast.LENGTH_SHORT).show()
                            playConfettiAnimation()
                         } else {
                            Toast.makeText(this@PlayerActivity, "Too late! $claimName already claimed.", Toast.LENGTH_SHORT).show()
                         }
                     }
                 })
             }
        } else {
            Toast.makeText(this, "Bogus Claim! $claimName conditions not met.", Toast.LENGTH_SHORT).show()
        }
    }

    // --- Claim Validation Logic ---
    private fun checkEarlyFive(): Boolean {
        // 1. Get the Player's Ticket Numbers: Get a simple list of all the numbers
        // that are actually on the player's ticket, filtering out the 0s used for blank spaces.
        if (currentTicket == null) return false
        val ticketNumbers = currentTicket!!.flatMap { it.toList() }.filter { it != 0 }.toSet()

        // 2. Find the "Valid Marked Numbers": Find the intersection of the ticket numbers
        // and the numbers the player has marked. A number is only valid if it's both
        // on the ticket AND has been marked by the player.
        val validMarkedNumbers = ticketNumbers.intersect(markedNumbers)

        // 3. Count and Verify: The claim is valid if the count is 5 or more.
        return validMarkedNumbers.size >= 5
    }

    private fun checkFourCorners(): Boolean {
        if (currentTicket == null) return false
        val topRow = currentTicket!![0].filter { it != 0 }
        val bottomRow = currentTicket!![2].filter { it != 0 }
        if (topRow.isEmpty() || bottomRow.isEmpty()) return false
        val corners = setOf(topRow.first(), topRow.last(), bottomRow.first(), bottomRow.last())
        return markedNumbers.containsAll(corners)
    }

    private fun checkLine(rowIndex: Int): Boolean {
        if (currentTicket == null) return false
        val lineNumbers = currentTicket!![rowIndex].filter { it != 0 }.toSet()
        return lineNumbers.isNotEmpty() && markedNumbers.containsAll(lineNumbers)
    }

    private fun checkFullHouse(): Boolean {
        if (currentTicket == null) return false
        val allTicketNumbers = currentTicket!!.flatMap { it.toList() }.filter { it != 0 }.toSet()
        return allTicketNumbers.isNotEmpty() && markedNumbers.containsAll(allTicketNumbers)
    }

    private fun displayTicket() {
        gridTicket.removeAllViews()
        if (currentTicket == null) return
        for (row in 0 until 3) {
            for (col in 0 until 9) {
                val value = currentTicket!![row][col]
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
                    val isCalled = calledNumbers.contains(value)
                    val isMarked = markedNumbers.contains(value)
                    styleNumberCell(tv, isCalled, isMarked)

                    tv.setOnClickListener {
                        if (isCalled) {
                            viewModel.markNumber(value)
                        } else {
                            Toast.makeText(this@PlayerActivity, "This number has not been called yet", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    tv.text = ""
                    styleEmptyCell(tv)
                }
                gridTicket.addView(tv)
            }
        }
    }

    private fun styleNumberCell(tv: TextView, isCalled: Boolean, isMarked: Boolean) {
         val drawable = GradientDrawable()
        drawable.shape = GradientDrawable.RECTANGLE
        drawable.cornerRadius = 16f

        if (isMarked) {
            drawable.setColor(Color.parseColor("#4CAF50")) // Green
            drawable.setStroke(4, Color.parseColor("#388E3C"))
            tv.setTextColor(Color.WHITE)
        } else {
            drawable.setColor(Color.WHITE)
            drawable.setStroke(2, Color.parseColor("#BDBDBD"))
            tv.setTextColor(Color.BLACK)
        }

        tv.background = drawable
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tv.elevation = if (isMarked) 2f else 6f
        }
    }

    private fun styleEmptyCell(tv: TextView) {
         val drawable = GradientDrawable()
        drawable.shape = GradientDrawable.RECTANGLE
        drawable.cornerRadius = 16f
        drawable.setColor(Color.parseColor("#E0E0E0"))
        tv.background = drawable
    }

    private fun playConfettiAnimation() {
        confettiAnimation.visibility = View.VISIBLE
        confettiAnimation.playAnimation()
        confettiAnimation.addAnimatorListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {}

            override fun onAnimationEnd(animation: Animator) {
                confettiAnimation.visibility = View.GONE
            }

            override fun onAnimationCancel(animation: Animator) {}

            override fun onAnimationRepeat(animation: Animator) {}
        })
    }
}

package com.example.tambola

import android.animation.Animator
import android.animation.ValueAnimator
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.airbnb.lottie.LottieAnimationView
import com.google.android.material.card.MaterialCardView
import androidx.recyclerview.widget.RecyclerView

class WinnerActivity : AppCompatActivity() {

    private lateinit var lottieWinner: LottieAnimationView
    private lateinit var cardWinners: MaterialCardView
    private lateinit var rvWinners: RecyclerView
    private lateinit var btnClose: Button
    private lateinit var tvWinnerTitle: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_winner)

        lottieWinner = findViewById(R.id.lottieWinner)
        cardWinners = findViewById(R.id.cardWinners)
        rvWinners = findViewById(R.id.rvWinners)
        btnClose = findViewById(R.id.btnClose)
        tvWinnerTitle = findViewById(R.id.tvWinnerTitle)

        val winnersList = intent.getSerializableExtra("winners") as? HashMap<String, String>

        btnClose.setOnClickListener { finish() }

        if (winnersList.isNullOrEmpty()) {
            tvWinnerTitle.text = "No Winners This Round!"
            lottieWinner.visibility = View.GONE // Hide animation if no winners
            cardWinners.visibility = View.VISIBLE
        } else {
            lottieWinner.addAnimatorListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) {}

                override fun onAnimationEnd(animation: Animator) {
                    animateLottieToTop()
                }

                override fun onAnimationCancel(animation: Animator) {}

                override fun onAnimationRepeat(animation: Animator) {}
            })
            setupRecyclerView(winnersList)
        }
    }

    private fun animateLottieToTop() {
        val valueAnimator = ValueAnimator.ofFloat(0.5f, 0.1f)
        valueAnimator.duration = 1000
        valueAnimator.addUpdateListener { animator ->
            val value = animator.animatedValue as Float
            val layoutParams = lottieWinner.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
            layoutParams.verticalBias = value
            lottieWinner.layoutParams = layoutParams
        }
        valueAnimator.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {}

            override fun onAnimationEnd(animation: Animator) {
                cardWinners.visibility = View.VISIBLE
            }

            override fun onAnimationCancel(animation: Animator) {}

            override fun onAnimationRepeat(animation: Animator) {}
        })
        valueAnimator.start()
    }

    private fun setupRecyclerView(winners: HashMap<String, String>) {
        val orderedClaims = listOf(
            "Full House",
            "Early Five",
            "Four Corners",
            "Top Line",
            "Middle Line",
            "Bottom Line"
        )

        val winnerDisplayList = orderedClaims.mapNotNull { claim ->
            winners[claim]?.let { winner ->
                Winner(claim, winner)
            }
        }

        rvWinners.layoutManager = LinearLayoutManager(this)
        rvWinners.adapter = WinnerAdapter(winnerDisplayList)
    }
}

package com.example.tambola

import android.animation.Animator
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.google.android.material.card.MaterialCardView

class WinnerAnimationManager(
    private val rootView: View,
    private val gameUiViews: List<View>
) {

    private val winnerOverlay: ConstraintLayout = rootView.findViewById(R.id.winnerOverlay)
    private val lottieWinner: LottieAnimationView = rootView.findViewById(R.id.lottieWinner)
    private val cardWinners: MaterialCardView = rootView.findViewById(R.id.cardWinners)
    private val rvWinners: RecyclerView = rootView.findViewById(R.id.rvWinners)
    private val btnClose: Button = rootView.findViewById(R.id.btnClose)
    private val tvWinnerTitle: TextView = rootView.findViewById(R.id.tvWinnerTitle)

    fun startWinnerSequence(winners: Map<String, String>, onFinished: () -> Unit) {
        // Hide all the game UI elements
        gameUiViews.forEach { it.visibility = View.GONE }

        // Set the close button listener
        btnClose.setOnClickListener { onFinished() }

        // Make the overlay visible
        winnerOverlay.visibility = View.VISIBLE

        if (winners.isEmpty()) {
            // If there are no winners, just show the message and the close button
            tvWinnerTitle.text = "No Winners This Round!"
            lottieWinner.visibility = View.GONE
            cardWinners.visibility = View.VISIBLE
            btnClose.visibility = View.VISIBLE
            return
        }

        // Set up the RecyclerView with the winners
        setupRecyclerView(winners)

        // Set up the Lottie animation listener
        lottieWinner.addAnimatorListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {}
            override fun onAnimationCancel(animation: Animator) {}
            override fun onAnimationRepeat(animation: Animator) {}

            override fun onAnimationEnd(animation: Animator) {
                showWinnerCard()
            }
        })

        // Start the Lottie animation
        lottieWinner.playAnimation()
    }

    private fun setupRecyclerView(winners: Map<String, String>) {
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

        rvWinners.adapter = WinnerAdapter(winnerDisplayList)
    }

    private fun showWinnerCard() {
        cardWinners.alpha = 0f
        cardWinners.visibility = View.VISIBLE
        cardWinners.translationY = 100f

        cardWinners.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(500)
            .withEndAction {
                // Show the close button after a short delay
                btnClose.postDelayed({ 
                    btnClose.alpha = 0f
                    btnClose.visibility = View.VISIBLE
                    btnClose.animate().alpha(1f).setDuration(300).start()
                }, 1000)
            }
            .start()
    }
}
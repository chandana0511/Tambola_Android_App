package com.example.tambola

import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

data class Winner(val claimType: String, val winnerName: String)

class WinnerAdapter(private val winners: List<Winner>) : RecyclerView.Adapter<WinnerAdapter.WinnerViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int):
WinnerViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_winner, parent, false)
        return WinnerViewHolder(view)
    }

    override fun onBindViewHolder(holder: WinnerViewHolder, position: Int) {
        val winner = winners[position]
        holder.bind(winner)
    }

    override fun getItemCount() = winners.size

    class WinnerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvClaimType: TextView = itemView.findViewById(R.id.tvClaimType)
        private val tvWinnerName: TextView = itemView.findViewById(R.id.tvWinnerName)
        private val winnerItemRoot: View = itemView.findViewById(R.id.winnerItemRoot)
        private val ivCrown: ImageView = itemView.findViewById(R.id.ivCrown)

        fun bind(winner: Winner) {
            tvClaimType.text = winner.claimType
            tvWinnerName.text = winner.winnerName

            if (winner.claimType == "Full House") {
                winnerItemRoot.setBackgroundResource(R.drawable.bg_full_house_winner)
                tvClaimType.setTextColor(Color.BLACK)
                tvWinnerName.setTextColor(Color.BLACK)
                tvWinnerName.setTypeface(tvWinnerName.typeface, Typeface.BOLD)
                tvClaimType.textSize = 20f
                tvWinnerName.textSize = 18f
                ivCrown.visibility = View.VISIBLE
            } else {
                winnerItemRoot.background = null
                tvClaimType.setTextColor(ContextCompat.getColor(itemView.context, R.color.text_secondary))
                tvWinnerName.setTextColor(ContextCompat.getColor(itemView.context, R.color.text_primary))
                tvWinnerName.setTypeface(null, Typeface.NORMAL)
                tvClaimType.textSize = 18f
                tvWinnerName.textSize = 16f
                ivCrown.visibility = View.GONE
            }
        }
    }
}

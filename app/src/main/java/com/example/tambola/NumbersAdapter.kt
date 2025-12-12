package com.example.tambola

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class NumbersAdapter(
    private val numbers: List<Int>,
    private val calledNumbers: Set<Int>
) : RecyclerView.Adapter<NumbersAdapter.NumberViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NumberViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_number_cell, parent, false)
        return NumberViewHolder(view)
    }

    override fun onBindViewHolder(holder: NumberViewHolder, position: Int) {
        holder.bind(numbers[position])
    }

    override fun getItemCount(): Int = numbers.size

    inner class NumberViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvNumber: TextView = itemView.findViewById(R.id.tvNumber)

        fun bind(number: Int) {
            tvNumber.text = number.toString()
            if (calledNumbers.contains(number)) {
                tvNumber.setBackgroundResource(R.drawable.bg_number_circle_selected)
                tvNumber.setTextColor(itemView.context.getColor(android.R.color.white))
            } else {
                tvNumber.setBackgroundResource(R.drawable.bg_number_circle_unselected)
                tvNumber.setTextColor(itemView.context.getColor(android.R.color.black))
            }
        }
    }
}

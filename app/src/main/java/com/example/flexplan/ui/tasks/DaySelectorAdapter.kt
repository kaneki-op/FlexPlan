package com.example.flexplan.ui.tasks

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.flexplan.R
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.*

class DaySelectorAdapter(
    private val days: List<Date>,
    private var initialSelectedDate: String,
    private val onDaySelected: (Date) -> Unit
) : RecyclerView.Adapter<DaySelectorAdapter.DayViewHolder>() {

    private var selectedPosition = -1

    init {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        selectedPosition = days.indexOfFirst { sdf.format(it) == initialSelectedDate }
        if (selectedPosition == -1) selectedPosition = 0
    }

    inner class DayViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val card: MaterialCardView = view.findViewById(R.id.cardDay)
        val tvDayName: TextView = view.findViewById(R.id.tvDayName)
        val tvDayNumber: TextView = view.findViewById(R.id.tvDayNumber)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_day_selector, parent, false)
        return DayViewHolder(view)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        val date = days[position]
        val dayNameFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val dayNumberFormat = SimpleDateFormat("dd", Locale.getDefault())

        holder.tvDayName.text = dayNameFormat.format(date)
        holder.tvDayNumber.text = dayNumberFormat.format(date)

        if (position == selectedPosition) {
            holder.card.setCardBackgroundColor(Color.parseColor("#FFD166")) // theme_accent
            holder.tvDayName.setTextColor(Color.parseColor("#210B2C")) // theme_bg
            holder.tvDayNumber.setTextColor(Color.parseColor("#210B2C"))
            holder.card.strokeWidth = 0
        } else {
            holder.card.setCardBackgroundColor(Color.parseColor("#2D132C")) // theme_surface
            holder.tvDayName.setTextColor(Color.parseColor("#A9ABB3")) // text_gray
            holder.tvDayNumber.setTextColor(Color.WHITE)
            holder.card.strokeWidth = 1
        }

        holder.itemView.setOnClickListener {
            val previousSelected = selectedPosition
            selectedPosition = holder.adapterPosition
            notifyItemChanged(previousSelected)
            notifyItemChanged(selectedPosition)
            onDaySelected(date)
        }
    }

    override fun getItemCount() = days.size
}

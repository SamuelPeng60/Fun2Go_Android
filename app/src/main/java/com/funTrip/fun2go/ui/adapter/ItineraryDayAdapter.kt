package com.funTrip.fun2go.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.funTrip.fun2go.R
import com.funTrip.fun2go.data.model.ItineraryDay

class ItineraryDayAdapter(
    private val onDayClick: (ItineraryDay) -> Unit = {}
) : RecyclerView.Adapter<ItineraryDayAdapter.DayViewHolder>() {

    private var days: List<ItineraryDay> = emptyList()

    fun submitList(newDays: List<ItineraryDay>) {
        days = newDays
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_itinerary_day, parent, false)
        return DayViewHolder(view)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        holder.bind(days[position])
    }

    override fun getItemCount() = days.size

    inner class DayViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvDayNumber: TextView = view.findViewById(R.id.tvDayNumber)
        private val tvDayDate: TextView = view.findViewById(R.id.tvDayDate)
        private val tvSpotCount: TextView = view.findViewById(R.id.tvSpotCount)

        fun bind(day: ItineraryDay) {
            tvDayNumber.text = "第${day.day_number}天"
            tvDayDate.text = day.date ?: "尚未設定日期"
            val count = day.spots?.size ?: 0
            tvSpotCount.text = "$count 個景點"
            itemView.setOnClickListener { onDayClick(day) }
        }
    }
}

package com.funTrip.fun2go.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.funTrip.fun2go.R
import com.funTrip.fun2go.data.model.Itinerary
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class ItineraryAdapter(
    private val onItemClick: (Itinerary) -> Unit,
    private val onEditClick: (Itinerary) -> Unit
) : RecyclerView.Adapter<ItineraryAdapter.ViewHolder>() {

    private var items: List<Itinerary> = emptyList()

    fun submitList(list: List<Itinerary>) {
        items = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_itinerary, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        private val tvDates: TextView = view.findViewById(R.id.tvDates)
        private val tvDayCount: TextView = view.findViewById(R.id.tvDayCount)
        private val btnEdit: ImageButton = view.findViewById(R.id.btnEdit)

        fun bind(itinerary: Itinerary) {
            tvTitle.text = itinerary.title

            val start = itinerary.start_date?.take(10)
            val end = itinerary.end_date?.take(10)
            tvDates.text = if (!start.isNullOrEmpty() || !end.isNullOrEmpty()) {
                "${start ?: "?"} ～ ${end ?: "?"}"
            } else {
                "未設定日期"
            }

            val dayCount: Int? = itinerary.days?.size ?: run {
                if (!start.isNullOrEmpty() && !end.isNullOrEmpty()) {
                    try {
                        val s = LocalDate.parse(start)
                        val e = LocalDate.parse(end)
                        (ChronoUnit.DAYS.between(s, e) + 1).toInt().coerceAtLeast(1)
                    } catch (ex: Exception) {
                        null
                    }
                } else {
                    null
                }
            }

            if (dayCount != null) {
                tvDayCount.text = "$dayCount 天"
                tvDayCount.visibility = View.VISIBLE
            } else {
                tvDayCount.visibility = View.GONE
            }

            itemView.setOnClickListener { onItemClick(itinerary) }
            btnEdit.setOnClickListener { onEditClick(itinerary) }
        }
    }
}

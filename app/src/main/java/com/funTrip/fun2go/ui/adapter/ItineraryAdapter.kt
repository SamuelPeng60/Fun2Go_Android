package com.funTrip.fun2go.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.funTrip.fun2go.R
import com.funTrip.fun2go.data.model.Itinerary

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

            tvDates.text = itinerary.destination?.takeIf { it.isNotEmpty() } ?: "未設定目的地"

            // 優先用實際 days.size（詳情頁回傳），否則用 total_days
            val dayCount = itinerary.days?.size?.takeIf { it > 0 } ?: itinerary.total_days.takeIf { it > 0 }
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

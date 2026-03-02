package com.funTrip.fun2go.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.funTrip.fun2go.R
import com.funTrip.fun2go.data.model.Itinerary
import com.google.android.material.button.MaterialButton

class PublicItineraryAdapter(
    private val onItemClick: (Itinerary) -> Unit,
    private val onCopyClick: (Itinerary) -> Unit
) : RecyclerView.Adapter<PublicItineraryAdapter.ViewHolder>() {

    private var items: List<Itinerary> = emptyList()

    fun submitList(list: List<Itinerary>) {
        items = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_public_itinerary, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        private val tvDestination: TextView = view.findViewById(R.id.tvDestination)
        private val tvDayCount: TextView = view.findViewById(R.id.tvDayCount)
        private val tvAuthor: TextView = view.findViewById(R.id.tvAuthor)
        private val tvCopyCount: TextView = view.findViewById(R.id.tvCopyCount)
        private val btnCopy: MaterialButton = view.findViewById(R.id.btnCopy)

        fun bind(itinerary: Itinerary) {
            tvTitle.text = itinerary.title
            tvDestination.text = itinerary.destination?.takeIf { it.isNotEmpty() } ?: "未設定目的地"

            val dayCount = itinerary.days?.size?.takeIf { it > 0 } ?: itinerary.total_days.takeIf { it > 0 }
            tvDayCount.text = if (dayCount != null) "$dayCount 天" else ""
            tvDayCount.visibility = if (dayCount != null) View.VISIBLE else View.GONE

            tvAuthor.text = if (!itinerary.authorName.isNullOrEmpty()) "by ${itinerary.authorName}" else ""
            tvAuthor.visibility = if (!itinerary.authorName.isNullOrEmpty()) View.VISIBLE else View.GONE

            tvCopyCount.text = if (itinerary.copy_count > 0) "被複製 ${itinerary.copy_count} 次" else ""
            tvCopyCount.visibility = if (itinerary.copy_count > 0) View.VISIBLE else View.GONE

            itemView.setOnClickListener { onItemClick(itinerary) }
            btnCopy.setOnClickListener { onCopyClick(itinerary) }
        }
    }
}

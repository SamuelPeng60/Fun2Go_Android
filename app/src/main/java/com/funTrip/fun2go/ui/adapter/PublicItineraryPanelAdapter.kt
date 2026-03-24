package com.funTrip.fun2go.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.funTrip.fun2go.R
import com.funTrip.fun2go.data.model.Itinerary
import com.google.android.material.button.MaterialButton

class PublicItineraryPanelAdapter(
    private val onItemClick: (Itinerary) -> Unit,
    private val onCopyClick: (Itinerary) -> Unit
) : RecyclerView.Adapter<PublicItineraryPanelAdapter.ViewHolder>() {

    private var items: List<Itinerary> = emptyList()

    fun submitList(list: List<Itinerary>) {
        items = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_public_itinerary_panel, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val ivCover: ImageView = view.findViewById(R.id.ivCover)
        private val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        private val tvDestination: TextView = view.findViewById(R.id.tvDestination)
        private val tvDays: TextView = view.findViewById(R.id.tvDays)
        private val tvAuthor: TextView = view.findViewById(R.id.tvAuthor)
        private val btnCopy: MaterialButton = view.findViewById(R.id.btnCopy)

        fun bind(itinerary: Itinerary) {
            ivCover.load(itinerary.coverImageUrl) {
                crossfade(true)
                placeholder(android.R.color.darker_gray)
                error(android.R.color.darker_gray)
            }
            tvTitle.text = itinerary.title

            val dest = itinerary.destination?.takeIf { it.isNotEmpty() }
            tvDestination.text = dest ?: ""
            tvDestination.visibility = if (dest != null) View.VISIBLE else View.GONE

            val dayCount = itinerary.days?.size?.takeIf { it > 0 }
                ?: itinerary.total_days.takeIf { it > 0 }
            tvDays.text = if (dayCount != null) "$dayCount 天" else ""
            tvDays.visibility = if (dayCount != null) View.VISIBLE else View.GONE

            val author = itinerary.authorName?.takeIf { it.isNotEmpty() }
            tvAuthor.text = if (author != null) "by $author" else ""
            tvAuthor.visibility = if (author != null) View.VISIBLE else View.GONE

            itemView.setOnClickListener { onItemClick(itinerary) }
            btnCopy.setOnClickListener { onCopyClick(itinerary) }
        }
    }
}

package com.funTrip.fun2go.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.funTrip.fun2go.R
import com.funTrip.fun2go.data.model.Spot

class SpotSearchAdapter(
    private val onItemClick: (Spot) -> Unit
) : RecyclerView.Adapter<SpotSearchAdapter.ViewHolder>() {

    private var items: List<Spot> = emptyList()

    fun submitList(list: List<Spot>) {
        items = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_spot_picker, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvName: TextView = view.findViewById(R.id.tvPickerSpotName)
        private val tvCategory: TextView = view.findViewById(R.id.tvPickerSpotCategory)

        fun bind(spot: Spot) {
            tvName.text = spot.name
            tvCategory.text = buildString {
                if (!spot.category.isNullOrBlank()) append(spot.category)
                if (!spot.address.isNullOrBlank()) {
                    if (isNotEmpty()) append("  ")
                    append(spot.address)
                }
            }
            itemView.setOnClickListener { onItemClick(spot) }
        }
    }
}

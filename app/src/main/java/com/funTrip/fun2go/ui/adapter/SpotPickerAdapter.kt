package com.funTrip.fun2go.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.funTrip.fun2go.R
import com.funTrip.fun2go.data.local.SavedSpotEntity

class SpotPickerAdapter(
    private val onSpotSelected: (SavedSpotEntity) -> Unit
) : RecyclerView.Adapter<SpotPickerAdapter.ViewHolder>() {

    private var spots: List<SavedSpotEntity> = emptyList()

    fun submitList(newSpots: List<SavedSpotEntity>) {
        spots = newSpots
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_spot_picker, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(spots[position])
    }

    override fun getItemCount() = spots.size

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvName: TextView = view.findViewById(R.id.tvPickerSpotName)
        private val tvCategory: TextView = view.findViewById(R.id.tvPickerSpotCategory)

        fun bind(spot: SavedSpotEntity) {
            tvName.text = spot.name
            tvCategory.text = spot.category ?: ""
            itemView.setOnClickListener { onSpotSelected(spot) }
        }
    }
}

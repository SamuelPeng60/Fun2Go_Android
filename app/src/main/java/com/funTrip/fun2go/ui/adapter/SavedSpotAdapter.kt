package com.funTrip.fun2go.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.funTrip.fun2go.R
import com.funTrip.fun2go.data.model.Spot

sealed class SavedListItem {
    data class SpotItem(val spot: Spot) : SavedListItem()
    data class DistanceSeparator(val distanceText: String, val durationText: String) : SavedListItem()
}

class SavedSpotAdapter(
    private val getCategoryLabel: (String?) -> String,
    private val onDelete: (Spot) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var items: List<SavedListItem> = emptyList()

    fun submitList(newItems: List<SavedListItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int) = when (items[position]) {
        is SavedListItem.SpotItem -> VIEW_TYPE_SPOT
        is SavedListItem.DistanceSeparator -> VIEW_TYPE_DISTANCE
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_SPOT -> SpotViewHolder(
                inflater.inflate(R.layout.item_saved_spot, parent, false)
            )
            else -> DistanceViewHolder(
                inflater.inflate(R.layout.item_distance_separator, parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is SavedListItem.SpotItem -> (holder as SpotViewHolder).bind(item.spot)
            is SavedListItem.DistanceSeparator -> (holder as DistanceViewHolder).bind(item)
        }
    }

    override fun getItemCount() = items.size

    inner class SpotViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvName: TextView = view.findViewById(R.id.tvSavedSpotName)
        private val tvCategory: TextView = view.findViewById(R.id.tvSavedSpotCategory)
        private val btnDelete: ImageButton = view.findViewById(R.id.btnDeleteSpot)

        fun bind(spot: Spot) {
            tvName.text = spot.name
            tvCategory.text = getCategoryLabel(spot.category)
            btnDelete.setOnClickListener { onDelete(spot) }
        }
    }

    inner class DistanceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvDistanceInfo: TextView = view.findViewById(R.id.tvDistanceInfo)

        fun bind(item: SavedListItem.DistanceSeparator) {
            tvDistanceInfo.text = "${item.distanceText}  ·  ${item.durationText}"
        }
    }

    companion object {
        private const val VIEW_TYPE_SPOT = 0
        private const val VIEW_TYPE_DISTANCE = 1
    }
}

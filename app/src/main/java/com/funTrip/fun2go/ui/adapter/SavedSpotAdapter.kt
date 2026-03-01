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
}

class SavedSpotAdapter(
    private val getCategoryLabel: (String?) -> String,
    private val onDelete: (Spot) -> Unit
) : RecyclerView.Adapter<SavedSpotAdapter.SpotViewHolder>() {

    private var items: List<Spot> = emptyList()

    fun submitList(newItems: List<SavedListItem>) {
        items = newItems.filterIsInstance<SavedListItem.SpotItem>().map { it.spot }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        SpotViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_saved_spot, parent, false))

    override fun onBindViewHolder(holder: SpotViewHolder, position: Int) = holder.bind(items[position])

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
}

package com.funTrip.fun2go.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.funTrip.fun2go.R
import com.funTrip.fun2go.data.model.Spot

class SavedSpotAdapter(
    private val spots: MutableList<Spot>,
    private val getCategoryLabel: (String?) -> String,
    private val onDelete: (Spot) -> Unit
) : RecyclerView.Adapter<SavedSpotAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvSavedSpotName)
        val tvCategory: TextView = view.findViewById(R.id.tvSavedSpotCategory)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDeleteSpot)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_saved_spot, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val spot = spots[position]
        holder.tvName.text = spot.name
        holder.tvCategory.text = getCategoryLabel(spot.category)
        holder.btnDelete.setOnClickListener { onDelete(spot) }
    }

    override fun getItemCount() = spots.size
}

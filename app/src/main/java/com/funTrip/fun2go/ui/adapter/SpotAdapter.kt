package com.funTrip.fun2go.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.funTrip.fun2go.R
import com.funTrip.fun2go.data.model.Spot

class SpotAdapter : RecyclerView.Adapter<SpotAdapter.SpotViewHolder>() {

    private var spotList = emptyList<Spot>()

    // 更新資料的方法
    fun submitList(newList: List<Spot>) {
        spotList = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SpotViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_spot, parent, false)
        return SpotViewHolder(view)
    }

    override fun onBindViewHolder(holder: SpotViewHolder, position: Int) {
        val spot = spotList[position]
        holder.bind(spot)
    }

    override fun getItemCount(): Int = spotList.size

    class SpotViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tvSpotName)
        private val tvCategory: TextView = itemView.findViewById(R.id.tvCategory)
        private val tvAddress: TextView = itemView.findViewById(R.id.tvAddress)
        private val tvRating: TextView = itemView.findViewById(R.id.tvRating)

        fun bind(spot: Spot) {
            tvName.text = spot.name
            tvCategory.text = spot.category ?: "未分類"
            tvAddress.text = spot.address ?: "無地址資訊"
            tvRating.text = if (spot.rating != null) "★ ${spot.rating}" else ""
        }
    }
}
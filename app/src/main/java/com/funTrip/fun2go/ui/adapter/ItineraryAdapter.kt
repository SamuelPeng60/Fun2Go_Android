package com.funTrip.fun2go.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.funTrip.fun2go.R
import com.funTrip.fun2go.data.model.Itinerary

class ItineraryAdapter(
    private val onItemClick: (Itinerary) -> Unit,
    private val onEditClick: (Itinerary) -> Unit,
    private val onDeleteClick: (Itinerary) -> Unit
) : RecyclerView.Adapter<ItineraryAdapter.ViewHolder>() {

    private var items: List<Itinerary> = emptyList()

    fun submitList(newList: List<Itinerary>) {
        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = items.size
            override fun getNewListSize() = newList.size
            override fun areItemsTheSame(oldPos: Int, newPos: Int) =
                items[oldPos].id == newList[newPos].id
            override fun areContentsTheSame(oldPos: Int, newPos: Int) =
                items[oldPos] == newList[newPos]
        })
        items = newList
        diff.dispatchUpdatesTo(this)
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
        private val ivCover: ImageView = view.findViewById(R.id.ivCover)
        private val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        private val tvDates: TextView = view.findViewById(R.id.tvDates)
        private val tvDayCount: TextView = view.findViewById(R.id.tvDayCount)
        private val tvPublicBadge: TextView = view.findViewById(R.id.tvPublicBadge)
        private val btnEdit: ImageButton = view.findViewById(R.id.btnEdit)
        private val btnDelete: ImageButton = view.findViewById(R.id.btnDelete)

        fun bind(itinerary: Itinerary) {
            val ctx = itemView.context

            // 封面圖
            val coverUrl = itinerary.coverImageUrl
            if (!coverUrl.isNullOrEmpty()) {
                ivCover.visibility = View.VISIBLE
                ivCover.load(coverUrl) { crossfade(true) }
            } else {
                ivCover.visibility = View.GONE
            }

            tvTitle.text = itinerary.title
            tvDates.text = itinerary.destination?.takeIf { it.isNotEmpty() }
                ?: ctx.getString(R.string.empty_destination)

            // 優先用實際 days.size（詳情頁回傳），否則用 total_days
            val dayCount = itinerary.days?.size?.takeIf { it > 0 } ?: itinerary.total_days.takeIf { it > 0 }
            if (dayCount != null) {
                tvDayCount.text = ctx.getString(R.string.format_day_count, dayCount)
                tvDayCount.visibility = View.VISIBLE
            } else {
                tvDayCount.visibility = View.GONE
            }

            // 已發佈標示
            tvPublicBadge.visibility = if (itinerary.is_public) View.VISIBLE else View.GONE

            itemView.setOnClickListener { onItemClick(itinerary) }
            btnEdit.setOnClickListener { onEditClick(itinerary) }
            btnDelete.setOnClickListener { onDeleteClick(itinerary) }
        }
    }
}

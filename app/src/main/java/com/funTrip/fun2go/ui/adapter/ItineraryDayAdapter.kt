package com.funTrip.fun2go.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.funTrip.fun2go.R
import com.funTrip.fun2go.data.model.ItineraryDay
import com.funTrip.fun2go.data.model.ItinerarySpot

class ItineraryDayAdapter(
    private val onAddSpotClick: (ItineraryDay) -> Unit = {},
    private val onRemoveSpotClick: (ItinerarySpot, dayId: Int) -> Unit = { _, _ -> }
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    sealed class Item {
        data class Header(val day: ItineraryDay, val isExpanded: Boolean) : Item()
        data class SpotEntry(val itSpot: ItinerarySpot, val dayId: Int) : Item()
        data class AddButton(val day: ItineraryDay) : Item()
    }

    companion object {
        private const val VIEW_DAY = 0
        private const val VIEW_SPOT = 1
        private const val VIEW_ADD = 2
    }

    private var days: List<ItineraryDay> = emptyList()
    private val expandedDayIds = mutableSetOf<Int>()
    private var flatList: List<Item> = emptyList()

    fun submitList(newDays: List<ItineraryDay>) {
        days = newDays
        rebuildList()
    }

    private fun rebuildList() {
        val list = mutableListOf<Item>()
        for (day in days) {
            val isExpanded = day.id in expandedDayIds
            list += Item.Header(day, isExpanded)
            if (isExpanded) {
                day.spots?.forEach { spot -> list += Item.SpotEntry(spot, day.id) }
                list += Item.AddButton(day)
            }
        }
        flatList = list
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int) = when (flatList[position]) {
        is Item.Header -> VIEW_DAY
        is Item.SpotEntry -> VIEW_SPOT
        is Item.AddButton -> VIEW_ADD
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_DAY -> DayHeaderViewHolder(inflater.inflate(R.layout.item_itinerary_day, parent, false))
            VIEW_SPOT -> SpotViewHolder(inflater.inflate(R.layout.item_day_spot, parent, false))
            VIEW_ADD -> AddButtonViewHolder(inflater.inflate(R.layout.item_day_add_spot, parent, false))
            else -> throw IllegalStateException("Unknown viewType: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = flatList[position]) {
            is Item.Header -> (holder as DayHeaderViewHolder).bind(item)
            is Item.SpotEntry -> (holder as SpotViewHolder).bind(item)
            is Item.AddButton -> (holder as AddButtonViewHolder).bind(item)
        }
    }

    override fun getItemCount() = flatList.size

    // ── Day Header ──────────────────────────────────────────────────────────
    inner class DayHeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvDayNumber: TextView = view.findViewById(R.id.tvDayNumber)
        private val tvDayDate: TextView = view.findViewById(R.id.tvDayDate)
        private val tvSpotCount: TextView = view.findViewById(R.id.tvSpotCount)
        private val ivChevron: ImageView = view.findViewById(R.id.ivChevron)

        fun bind(item: Item.Header) {
            val day = item.day
            tvDayNumber.text = "第${day.day_number}天"
            tvDayDate.text = day.date ?: "尚未設定日期"
            val count = day.spots?.size ?: 0
            tvSpotCount.text = "$count 個景點"
            ivChevron.rotation = if (item.isExpanded) 90f else 0f

            itemView.setOnClickListener {
                if (day.id in expandedDayIds) expandedDayIds.remove(day.id)
                else expandedDayIds.add(day.id)
                rebuildList()
            }
        }
    }

    // ── Spot Item ────────────────────────────────────────────────────────────
    inner class SpotViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvSpotName: TextView = view.findViewById(R.id.tvSpotName)
        private val tvSpotNote: TextView = view.findViewById(R.id.tvSpotNote)
        private val btnRemove: ImageButton = view.findViewById(R.id.btnRemoveSpot)

        fun bind(item: Item.SpotEntry) {
            val itSpot = item.itSpot
            tvSpotName.text = itSpot.spot_detail?.name ?: "景點 #${itSpot.spot_id}"

            val noteText = buildString {
                itSpot.arrival_time?.let { append("抵達：$it") }
                if (!itSpot.arrival_time.isNullOrBlank() && !itSpot.note.isNullOrBlank()) append("　")
                itSpot.note?.takeIf { it.isNotBlank() }?.let { append(it) }
            }
            if (noteText.isNotBlank()) {
                tvSpotNote.visibility = View.VISIBLE
                tvSpotNote.text = noteText
            } else {
                tvSpotNote.visibility = View.GONE
            }

            btnRemove.setOnClickListener { onRemoveSpotClick(itSpot, item.dayId) }
        }
    }

    // ── Add Spot Button ──────────────────────────────────────────────────────
    inner class AddButtonViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bind(item: Item.AddButton) {
            itemView.setOnClickListener { onAddSpotClick(item.day) }
        }
    }
}

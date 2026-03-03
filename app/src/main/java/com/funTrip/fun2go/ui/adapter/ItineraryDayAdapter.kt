package com.funTrip.fun2go.ui.adapter

import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.funTrip.fun2go.R
import com.funTrip.fun2go.data.model.ItineraryDay
import com.funTrip.fun2go.data.model.ItinerarySpot
import kotlin.math.*

class ItineraryDayAdapter(
    private val onAddSpotClick: (ItineraryDay) -> Unit = {},
    private val onRemoveSpotClick: (ItinerarySpot, dayId: Int) -> Unit = { _, _ -> },
    private val onDateClick: (ItineraryDay) -> Unit = {}
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    sealed class Item {
        data class Header(val day: ItineraryDay, val isExpanded: Boolean) : Item()
        data class SpotEntry(val itSpot: ItinerarySpot, val dayId: Int) : Item()
        data class DistanceSeparator(val distText: String, val durationText: String) : Item()
        data class AddButton(val day: ItineraryDay) : Item()
    }

    companion object {
        private const val VIEW_DAY = 0
        private const val VIEW_SPOT = 1
        private const val VIEW_ADD = 2
        private const val VIEW_DISTANCE = 3

        private fun calcDistance(a: ItinerarySpot, b: ItinerarySpot): Pair<String, String>? {
            val lat1 = a.spot_detail?.latitude?.toDoubleOrNull() ?: return null
            val lng1 = a.spot_detail?.longitude?.toDoubleOrNull() ?: return null
            val lat2 = b.spot_detail?.latitude?.toDoubleOrNull() ?: return null
            val lng2 = b.spot_detail?.longitude?.toDoubleOrNull() ?: return null
            val R = 6371.0
            val dLat = Math.toRadians(lat2 - lat1)
            val dLng = Math.toRadians(lng2 - lng1)
            val av = sin(dLat / 2).pow(2) +
                    cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2).pow(2)
            val roadKm = R * 2 * atan2(sqrt(av), sqrt(1 - av)) * 1.3
            val minutes = (roadKm / 25.0 * 60).toInt().coerceAtLeast(1)
            val distText = when {
                roadKm < 1.0  -> "${(roadKm * 1000).toInt()} 公尺"
                roadKm < 10.0 -> "%.1f 公里".format(roadKm)
                else          -> "${roadKm.toInt()} 公里"
            }
            val durationText = if (minutes < 60) "約 $minutes 分鐘"
            else { val h = minutes / 60; val m = minutes % 60
                if (m == 0) "約 $h 小時" else "約 ${h}h${m}m" }
            return distText to durationText
        }
    }

    var itemTouchHelper: ItemTouchHelper? = null

    private var days: List<ItineraryDay> = emptyList()
    private val expandedDayIds = mutableSetOf<Int>()
    private var flatList: List<Item> = emptyList()
    private var dragDayId: Int? = null

    fun onSpotMoved(from: Int, to: Int): Boolean {
        val fromItem = flatList.getOrNull(from) as? Item.SpotEntry ?: return false
        val toItem   = flatList.getOrNull(to)   as? Item.SpotEntry ?: return false
        if (fromItem.dayId != toItem.dayId) return false

        dragDayId = fromItem.dayId

        val newList = flatList.toMutableList()
        newList.removeAt(from)
        newList.add(to, fromItem)
        flatList = newList
        notifyItemMoved(from, to)

        val dayId = fromItem.dayId
        val newSpotOrder = flatList.filterIsInstance<Item.SpotEntry>()
            .filter { it.dayId == dayId }.map { it.itSpot }
        val dayIndex = days.indexOfFirst { it.id == dayId }
        if (dayIndex >= 0)
            days = days.toMutableList().also { it[dayIndex] = days[dayIndex].copy(spots = newSpotOrder) }

        return true
    }

    fun commitDragOrder(onReorder: (dayId: Int, spotIds: List<Int>) -> Unit) {
        dragDayId?.let { dayId ->
            val spotIds = days.firstOrNull { it.id == dayId }?.spots?.map { it.id }
            if (spotIds != null) onReorder(dayId, spotIds)
        }
        dragDayId = null
        rebuildList()
    }

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
                val spots = day.spots ?: emptyList()
                spots.forEachIndexed { index, itSpot ->
                    list += Item.SpotEntry(itSpot, day.id)
                    if (index < spots.size - 1) {
                        val dist = calcDistance(itSpot, spots[index + 1])
                        if (dist != null) list += Item.DistanceSeparator(dist.first, dist.second)
                    }
                }
                list += Item.AddButton(day)
            }
        }
        flatList = list
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int) = when (flatList[position]) {
        is Item.Header -> VIEW_DAY
        is Item.SpotEntry -> VIEW_SPOT
        is Item.DistanceSeparator -> VIEW_DISTANCE
        is Item.AddButton -> VIEW_ADD
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_DAY -> DayHeaderViewHolder(inflater.inflate(R.layout.item_itinerary_day, parent, false))
            VIEW_SPOT -> SpotViewHolder(inflater.inflate(R.layout.item_day_spot, parent, false))
            VIEW_DISTANCE -> DistanceSeparatorViewHolder(inflater.inflate(R.layout.item_distance_separator, parent, false))
            VIEW_ADD -> AddButtonViewHolder(inflater.inflate(R.layout.item_day_add_spot, parent, false))
            else -> throw IllegalStateException("Unknown viewType: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = flatList[position]) {
            is Item.Header -> (holder as DayHeaderViewHolder).bind(item)
            is Item.SpotEntry -> (holder as SpotViewHolder).bind(item)
            is Item.DistanceSeparator -> (holder as DistanceSeparatorViewHolder).bind(item)
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
            if (day.date.isNullOrBlank()) {
                tvDayDate.text = "點擊設定日期"
                tvDayDate.setTextColor(android.graphics.Color.parseColor("#F44062"))
            } else {
                tvDayDate.text = day.date
                tvDayDate.setTextColor(android.graphics.Color.parseColor("#333333"))
            }
            val count = day.spots?.size ?: 0
            tvSpotCount.text = "$count 個景點"
            ivChevron.rotation = if (item.isExpanded) 90f else 0f

            tvDayDate.setOnClickListener { onDateClick(day) }

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
        private val ivDragHandle: ImageView = view.findViewById(R.id.ivDragHandle)

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

            ivDragHandle.setOnTouchListener { _, event ->
                if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                    itemTouchHelper?.startDrag(this@SpotViewHolder)
                }
                false
            }
        }
    }

    // ── Distance Separator ───────────────────────────────────────────────────
    inner class DistanceSeparatorViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvDistanceInfo: TextView = view.findViewById(R.id.tvDistanceInfo)

        fun bind(item: Item.DistanceSeparator) {
            tvDistanceInfo.text = "${item.distText}  ·  ${item.durationText}"
        }
    }

    // ── Add Spot Button ──────────────────────────────────────────────────────
    inner class AddButtonViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bind(item: Item.AddButton) {
            itemView.setOnClickListener { onAddSpotClick(item.day) }
        }
    }
}

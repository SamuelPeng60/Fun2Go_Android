package com.funTrip.fun2go.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.funTrip.fun2go.R
import com.funTrip.fun2go.data.model.Itinerary
import com.google.android.material.button.MaterialButton

class PublicItineraryAdapter(
    private val onItemClick: (Itinerary) -> Unit,
    private val onCopyClick: (Itinerary) -> Unit,
    private val onAuthorClick: ((Itinerary) -> Unit)? = null
) : RecyclerView.Adapter<PublicItineraryAdapter.ViewHolder>() {

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
            .inflate(R.layout.item_public_itinerary, parent, false)
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
        private val tvDayCount: TextView = view.findViewById(R.id.tvDayCount)
        private val llAuthor: android.widget.LinearLayout = view.findViewById(R.id.llAuthor)
        private val ivAuthorAvatar: ImageView = view.findViewById(R.id.ivAuthorAvatar)
        private val tvAuthor: TextView = view.findViewById(R.id.tvAuthor)
        private val tvCopyCount: TextView = view.findViewById(R.id.tvCopyCount)
        private val btnCopy: MaterialButton = view.findViewById(R.id.btnCopy)

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
            tvDestination.text = itinerary.destination?.takeIf { it.isNotEmpty() }
                ?: ctx.getString(R.string.empty_destination)

            val dayCount = itinerary.days?.size?.takeIf { it > 0 } ?: itinerary.total_days.takeIf { it > 0 }
            tvDayCount.text = if (dayCount != null) ctx.getString(R.string.format_day_count, dayCount) else ""
            tvDayCount.visibility = if (dayCount != null) View.VISIBLE else View.GONE

            // 作者頭像
            val avatarUrl = itinerary.authorAvatar
            if (!avatarUrl.isNullOrEmpty()) {
                ivAuthorAvatar.visibility = View.VISIBLE
                ivAuthorAvatar.load(avatarUrl) {
                    crossfade(true)
                    transformations(CircleCropTransformation())
                }
            } else {
                ivAuthorAvatar.visibility = View.GONE
            }

            tvAuthor.text = if (!itinerary.authorName.isNullOrEmpty()) "by ${itinerary.authorName}" else ""
            tvAuthor.visibility = if (!itinerary.authorName.isNullOrEmpty()) View.VISIBLE else View.GONE
            if (onAuthorClick != null && !itinerary.authorName.isNullOrEmpty()) {
                llAuthor.setOnClickListener { onAuthorClick.invoke(itinerary) }
            } else {
                llAuthor.setOnClickListener(null)
            }

            tvCopyCount.text = if (itinerary.copy_count > 0)
                ctx.getString(R.string.format_copy_count, itinerary.copy_count) else ""
            tvCopyCount.visibility = if (itinerary.copy_count > 0) View.VISIBLE else View.GONE

            itemView.setOnClickListener { onItemClick(itinerary) }
            btnCopy.setOnClickListener { onCopyClick(itinerary) }
        }
    }
}

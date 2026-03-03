package com.funTrip.fun2go.ui.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.funTrip.fun2go.R
import com.funTrip.fun2go.data.model.Order
import java.text.NumberFormat
import java.util.Locale

class OrderAdapter(
    private val onItemClick: (Order) -> Unit
) : RecyclerView.Adapter<OrderAdapter.ViewHolder>() {

    private var items: List<Order> = emptyList()

    fun submitList(list: List<Order>) {
        items = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_order, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvOrderId: TextView     = view.findViewById(R.id.tvOrderId)
        private val tvStatus: TextView      = view.findViewById(R.id.tvStatus)
        private val tvVehicleName: TextView = view.findViewById(R.id.tvVehicleName)
        private val tvPickupTime: TextView  = view.findViewById(R.id.tvPickupTime)
        private val tvAmount: TextView      = view.findViewById(R.id.tvAmount)

        fun bind(order: Order) {
            tvOrderId.text = itemView.context.getString(R.string.format_order_id, order.id)

            val ctx = itemView.context
            val (statusText, statusColor) = when (order.status) {
                "pending"   -> Pair(ctx.getString(R.string.status_pending),   Color.parseColor("#FF9800"))
                "confirmed" -> Pair(ctx.getString(R.string.status_confirmed), Color.parseColor("#4CAF50"))
                "completed" -> Pair(ctx.getString(R.string.status_completed), Color.parseColor("#9E9E9E"))
                "cancelled" -> Pair(ctx.getString(R.string.status_cancelled), Color.parseColor("#F44336"))
                else        -> Pair(order.status, Color.parseColor("#9E9E9E"))
            }
            tvStatus.text = statusText
            tvStatus.setTextColor(statusColor)

            val booking = order.charterBooking
            tvVehicleName.text = booking?.vehicleName ?: itemView.context.getString(R.string.label_charter_order)

            tvPickupTime.text = booking?.pickupTime
                ?.replace("T", " ")
                ?.take(16)
                ?: ""

            val amount = order.totalAmount.toDoubleOrNull()?.toLong()
            tvAmount.text = if (amount != null) {
                "NT$ ${NumberFormat.getNumberInstance(Locale.US).format(amount)}"
            } else {
                "NT$ ${order.totalAmount}"
            }

            itemView.setOnClickListener { onItemClick(order) }
        }
    }
}

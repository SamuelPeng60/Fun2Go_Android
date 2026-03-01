package com.funTrip.fun2go.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.funTrip.fun2go.R
import com.funTrip.fun2go.data.model.Vehicle
import java.text.NumberFormat
import java.util.Locale

class VehicleAdapter(
    private val onItemClick: (Vehicle) -> Unit
) : RecyclerView.Adapter<VehicleAdapter.ViewHolder>() {

    private var items: List<Vehicle> = emptyList()

    fun submitList(list: List<Vehicle>) {
        items = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_vehicle, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val ivVehicle: ImageView  = view.findViewById(R.id.ivVehicle)
        private val tvVehicleName: TextView = view.findViewById(R.id.tvVehicleName)
        private val tvType: TextView      = view.findViewById(R.id.tvType)
        private val tvCapacity: TextView  = view.findViewById(R.id.tvCapacity)
        private val tvPrice: TextView     = view.findViewById(R.id.tvPrice)
        private val tvAvailable: TextView = view.findViewById(R.id.tvAvailable)

        fun bind(vehicle: Vehicle) {
            tvVehicleName.text = vehicle.name
            tvType.text = typeDisplayName(vehicle.type)
            tvCapacity.text = "最多 ${vehicle.capacity} 人"

            val price = vehicle.pricePerDay.toDoubleOrNull()?.toLong()
            tvPrice.text = if (price != null) {
                "NT$ ${NumberFormat.getNumberInstance(Locale.US).format(price)} / 天"
            } else {
                "NT$ ${vehicle.pricePerDay} / 天"
            }

            if (vehicle.isAvailable) {
                tvAvailable.visibility = View.GONE
            } else {
                tvAvailable.visibility = View.VISIBLE
            }

            ivVehicle.load(vehicle.imageUrl) {
                placeholder(android.R.color.darker_gray)
                error(android.R.color.darker_gray)
            }

            itemView.setOnClickListener { onItemClick(vehicle) }
        }

        private fun typeDisplayName(type: String) = when (type) {
            "sedan_4" -> "轎車"
            "van_9"   -> "九人座"
            "bus_20"  -> "巴士"
            else      -> type
        }
    }
}

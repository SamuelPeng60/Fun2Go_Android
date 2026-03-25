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
            tvCapacity.text = itemView.context.getString(R.string.format_capacity, vehicle.capacity)

            val price = vehicle.pricePerDay.toDoubleOrNull()?.toLong()
            val priceStr = if (price != null) NumberFormat.getNumberInstance(Locale.US).format(price)
                           else vehicle.pricePerDay
            tvPrice.text = itemView.context.getString(R.string.format_price_per_day, priceStr)

            if (vehicle.isAvailable) {
                tvAvailable.visibility = View.GONE
            } else {
                tvAvailable.visibility = View.VISIBLE
            }

            val imageUrl = vehicle.imageUrl?.replace("http://", "https://")
            ivVehicle.load(imageUrl) {
                crossfade(true)
                placeholder(android.R.color.darker_gray)
                error(android.R.color.darker_gray)
            }

            itemView.setOnClickListener { onItemClick(vehicle) }
        }

        private fun typeDisplayName(type: String) = when (type) {
            "sedan_4" -> itemView.context.getString(R.string.vehicle_type_sedan)
            "van_9"   -> itemView.context.getString(R.string.vehicle_type_van)
            "bus_20"  -> itemView.context.getString(R.string.vehicle_type_bus)
            else      -> type
        }
    }
}

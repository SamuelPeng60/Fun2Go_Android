package com.funTrip.fun2go.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.funTrip.fun2go.data.model.Spot

@Entity(tableName = "saved_spots")
data class SavedSpotEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val category: String? = null,
    val latitude: String? = null,
    val longitude: String? = null,
    val address: String? = null,
    val image_url: String? = null,
    val rating: Double? = null
) {
    fun toSpot() = Spot(id, name, category, latitude, longitude, address, image_url, null, rating)
}

fun Spot.toEntity() = SavedSpotEntity(id, name, category, latitude, longitude, address, image_url, rating)

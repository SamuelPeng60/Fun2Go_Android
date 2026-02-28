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
    fun toSpot() = Spot(
        id = id, name = name, category = category,
        latitude = latitude, longitude = longitude,
        address = address, image_url = image_url,
        rating = rating
    )
}

fun Spot.toEntity() = SavedSpotEntity(
    id = id, name = name, category = category,
    latitude = latitude, longitude = longitude,
    address = address, image_url = image_url,
    rating = rating
)

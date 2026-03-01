package com.funTrip.fun2go.data.model

import com.google.gson.annotations.SerializedName

data class Vehicle(
    val id: Int,
    val name: String,
    val type: String,
    val capacity: Int,
    @SerializedName("price_per_day") val pricePerDay: String,
    @SerializedName("image_url")     val imageUrl: String?,
    val description: String?,
    @SerializedName("is_available")  val isAvailable: Boolean
)

package com.funTrip.fun2go.data.model

data class Spot(
    val id: Int,
    val name: String,
    val description: String?,
    val category: String?,
    val lat: Double,
    val lng: Double,
    val address: String?,
    val image_url: String?,
    val rating: Double?
)
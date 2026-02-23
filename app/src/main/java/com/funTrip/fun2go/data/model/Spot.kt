package com.funTrip.fun2go.data.model

data class Spot(
    val id: Int,
    val name: String,
    val category: String? = null,
    val latitude: String? = null,
    val longitude: String? = null,
    val address: String? = null,
    val image_url: String? = null,
    // 以下欄位 API 不回傳，保留供建立景點或顯示使用
    val description: String? = null,
    val rating: Double? = null
)
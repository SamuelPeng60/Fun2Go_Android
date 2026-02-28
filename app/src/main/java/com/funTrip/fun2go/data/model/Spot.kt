package com.funTrip.fun2go.data.model

import com.google.gson.annotations.SerializedName

data class Spot(
    val id: Int,
    val name: String,
    val category: String? = null,
    val latitude: String? = null,
    val longitude: String? = null,
    val address: String? = null,
    val image_url: String? = null,
    // v1.6：景點擁有權與可見性
    @SerializedName("creator_id") val creatorId: Int? = null,
    @SerializedName("is_public") val isPublic: Boolean = false,
    val source: String? = null,   // "official" 或 "user"
    // 以下欄位 API 不回傳，保留供建立景點或顯示使用
    val description: String? = null,
    val rating: Double? = null
)
package com.funTrip.fun2go.data.model

import com.google.gson.annotations.SerializedName

data class Itinerary(
    val id: Int,
    val title: String,
    @SerializedName(value = "user_id", alternate = ["author_id"]) val author_id: Int = 0,
    val destination: String? = null,
    val total_days: Int = 1,
    val copy_count: Int = 0,
    val is_public: Boolean = false,
    @SerializedName("cover_image_url") val coverImageUrl: String? = null,
    // 詳情 API 回傳巢狀天數
    val days: List<ItineraryDay>? = null,
    // 列表 API 回傳平鋪的作者欄位
    @SerializedName("author_name") val authorName: String? = null,
    @SerializedName("author_avatar") val authorAvatar: String? = null
)

data class ItineraryDay(
    // getItineraryDetail 嵌套回傳 "day_id"，直接 CRUD 回傳 "id"
    @SerializedName(value = "day_id", alternate = ["id"]) val id: Int,
    val itinerary_id: Int = 0,
    val day_number: Int,
    val date: String?,
    val spots: List<ItinerarySpot>? = null
)

data class ItinerarySpot(
    // getItineraryDetail 嵌套回傳 "itinerary_spot_id"，直接 CRUD 回傳 "id"
    @SerializedName(value = "itinerary_spot_id", alternate = ["id"]) val id: Int,
    val spot_id: Int = 0,
    val day_id: Int = 0,
    val order_index: Int = 0,
    val arrival_time: String? = null,
    val departure_time: String? = null,
    val duration_minutes: Int? = null,
    val note: String? = null,
    // getItineraryDetail 嵌套回傳 "spot"，舊欄位名稱 "spot_detail" 保留相容
    @SerializedName(value = "spot", alternate = ["spot_detail"]) val spot_detail: Spot? = null
)

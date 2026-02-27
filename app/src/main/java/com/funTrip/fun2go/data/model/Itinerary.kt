package com.funTrip.fun2go.data.model

import com.google.gson.annotations.SerializedName

data class Itinerary(
    val id: Int,
    val title: String,
    @SerializedName("user_id") val author_id: Int = 0,
    val start_date: String?,
    val end_date: String?,
    val copy_count: Int = 0,
    val is_public: Boolean = false,
    val cover_image: String?,
    // 詳情 API 可能會回傳巢狀結構
    val days: List<ItineraryDay>? = null,
    val author: User? = null
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
    val note: String? = null,
    // getItineraryDetail 嵌套回傳 "spot"，舊欄位名稱 "spot_detail" 保留相容
    @SerializedName(value = "spot", alternate = ["spot_detail"]) val spot_detail: Spot? = null
)

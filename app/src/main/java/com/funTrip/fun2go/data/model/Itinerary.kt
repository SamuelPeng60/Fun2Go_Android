package com.funTrip.fun2go.data.model

data class Itinerary(
    val id: Int,
    val title: String,
    val author_id: Int,
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
    val id: Int,
    val itinerary_id: Int,
    val day_number: Int,
    val date: String?,
    val spots: List<ItinerarySpot>? = null
)

data class ItinerarySpot(
    val id: Int, // 關聯表的 ID
    val spot_id: Int,
    val day_id: Int,
    val order_index: Int,
    val arrival_time: String?,
    val note: String?,
    val spot_detail: Spot? // 透過 JOIN 抓回來的景點詳細資料
)
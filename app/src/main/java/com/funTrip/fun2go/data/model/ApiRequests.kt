package com.funTrip.fun2go.data.model

// 建立/更新 用戶
data class UserRequest(val name: String, val email: String?)

// 建立/更新 行程
data class ItineraryRequest(
    val title: String,
    val start_date: String?,
    val end_date: String?,
    val is_public: Boolean? = false
)

// 新增景點到行程的一天
data class AddSpotToDayRequest(
    val spot_id: Int,
    val order_index: Int,
    val arrival_time: String?,
    val note: String?
)

// 收藏
data class FavoriteRequest(val user_id: Int, val spot_id: Int)
data class UnfavoriteRequest(val user_id: Int) // DELETE body 比較特殊，通常用 Query 或 Path，但你的 API 寫 body

// 新增天數
data class AddDayRequest(val day_number: Int)

// 排序
data class ReorderRequest(val spot_ids: List<Int>) // 假設傳送新的 ID 順序陣列
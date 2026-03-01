package com.funTrip.fun2go.data.repository

import com.funTrip.fun2go.data.model.*
import com.funTrip.fun2go.data.remote.NetworkResult
import com.funTrip.fun2go.data.remote.RetrofitClient

class TripRepository : BaseRepository() {

    private val api = RetrofitClient.apiService

    // --- Auth ---

    suspend fun loginWithGoogle(idToken: String) = safeApiCall {
        api.loginWithGoogle(GoogleAuthRequest(idToken))
    }

    suspend fun refreshToken(refreshToken: String) = safeApiCall {
        api.refreshToken(RefreshTokenRequest(refreshToken))
    }

    suspend fun logout(refreshToken: String) = safeApiCall {
        api.logout(LogoutRequest(refreshToken))
    }

    // 抓取單個用戶
    suspend fun getUser(userId: Int) = safeApiCall {
        api.getUser(userId)
    }

    // 搜尋景點
    suspend fun searchSpots(keyword: String) = safeApiCall {
        api.searchSpots(keyword = keyword)
    }

    // 取得公開行程列表
    suspend fun getItineraries(limit: Int, offset: Int) = safeApiCall {
        api.getItineraries(limit, offset)
    }

    // 取得行程詳細內容
    suspend fun getItineraryDetail(id: Int) = safeApiCall {
        api.getItineraryDetail(id)
    }

    // 建立行程
    suspend fun createItinerary(title: String, totalDays: Int?, destination: String?): NetworkResult<Itinerary> {
        val result = safeApiCall {
            api.createItinerary(ItineraryRequest(title, totalDays, destination?.ifEmpty { null }))
        }
        // 後端 bug 防護：行程建立成功但 user_id=null → 無法新增天數(403)
        if (result is NetworkResult.Success && (result.data?.author_id ?: 0) == 0) {
            return NetworkResult.Error("行程建立異常：伺服器未能設置擁有者，請重新嘗試")
        }
        return result
    }

    // --- Users ---

    suspend fun createUser(name: String, email: String?) = safeApiCall {
        api.createUser(UserRequest(name, email))
    }

    suspend fun updateUser(id: Int, name: String, email: String?) = safeApiCall {
        api.updateUser(id, UserRequest(name, email))
    }

    suspend fun getUserItineraries(userId: Int) = safeApiCall {
        api.getUserItineraries(userId)
    }

    suspend fun getUserFavorites(userId: Int) = safeApiCall {
        api.getUserFavorites(userId)
    }

    // --- Itineraries ---

    suspend fun updateItinerary(id: Int, title: String, totalDays: Int?, destination: String?) = safeApiCall {
        api.updateItinerary(id, ItineraryRequest(title, totalDays, destination?.ifEmpty { null }))
    }

    suspend fun deleteItinerary(id: Int) = safeApiCall { api.deleteItinerary(id) }

    suspend fun copyItinerary(id: Int) = safeApiCall { api.copyItinerary(id) }

    suspend fun publishItinerary(id: Int) = safeApiCall { api.publishItinerary(id) }

    // --- Itinerary Days ---

    suspend fun initItineraryDays(itineraryId: Int, totalDays: Int, startDate: String?): NetworkResult<Unit> {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val startCal: java.util.Calendar? = startDate?.let { dateStr ->
            runCatching {
                java.util.Calendar.getInstance().also { it.time = sdf.parse(dateStr)!! }
            }.getOrNull()
        }
        for (dayNumber in 1..totalDays) {
            val result = addDay(itineraryId, dayNumber)
            if (result is NetworkResult.Success) {
                val dayId = result.data?.id ?: continue
                if (startCal != null) {
                    val cal = java.util.Calendar.getInstance().also { it.time = startCal.time }
                    cal.add(java.util.Calendar.DAY_OF_MONTH, dayNumber - 1)
                    updateDay(itineraryId, dayId, mapOf("date" to sdf.format(cal.time)))
                }
            } else if (result is NetworkResult.Error) {
                return NetworkResult.Error("建立第 $dayNumber 天失敗")
            }
        }
        return NetworkResult.Success(Unit)
    }

    suspend fun addDay(itineraryId: Int, dayNumber: Int) = safeApiCall {
        api.addDay(itineraryId, AddDayRequest(dayNumber))
    }

    suspend fun updateDay(itineraryId: Int, dayId: Int, data: Map<String, String>) = safeApiCall {
        api.updateDay(itineraryId, dayId, data)
    }

    suspend fun deleteDay(itineraryId: Int, dayId: Int) = safeApiCall {
        api.deleteDay(itineraryId, dayId)
    }

    // --- Itinerary Spots ---

    suspend fun addSpotToDay(dayId: Int, request: AddSpotToDayRequest) = safeApiCall {
        api.addSpotToDay(dayId, request)
    }

    suspend fun updateSpotInDay(dayId: Int, spotId: Int, request: AddSpotToDayRequest) = safeApiCall {
        api.updateSpotInDay(dayId, spotId, request)
    }

    suspend fun removeSpotFromDay(dayId: Int, spotId: Int) = safeApiCall {
        api.removeSpotFromDay(dayId, spotId)
    }

    suspend fun reorderSpots(dayId: Int, spotIds: List<Int>) = safeApiCall {
        api.reorderSpots(dayId, ReorderRequest(spotIds))
    }

    // --- Spots ---

    suspend fun getAllSpots() = safeApiCall {
        api.searchSpots() // 不帶 keyword，回傳全部景點
    }

    suspend fun getSpotDetail(id: Int) = safeApiCall { api.getSpotDetail(id) }

    suspend fun createSpot(req: SpotRequest) = safeApiCall { api.createSpot(req) }

    suspend fun updateSpot(id: Int, req: SpotRequest) = safeApiCall { api.updateSpot(id, req) }

    suspend fun deleteSpot(id: Int): NetworkResult<Unit> = safeApiCall { api.deleteSpot(id) }

    // --- Vehicles ---

    suspend fun getVehicles(type: String?, available: Boolean?) = safeApiCall {
        api.getVehicles(type, available)
    }

    suspend fun getVehicleDetail(id: Int) = safeApiCall {
        api.getVehicleDetail(id)
    }

    // --- Favorites ---

    suspend fun addFavorite(userId: Int, spotId: Int) = safeApiCall {
        api.addFavorite(FavoriteRequest(userId, spotId))
    }

    suspend fun removeFavorite(spotId: Int, userId: Int) = safeApiCall {
        api.removeFavorite(spotId, UnfavoriteRequest(userId))
    }

    // --- Orders ---

    suspend fun createOrder(req: CreateOrderRequest) = safeApiCall { api.createOrder(req) }

    suspend fun getOrders(status: String?) = safeApiCall { api.getOrders(status) }

    suspend fun getOrderDetail(id: Int) = safeApiCall { api.getOrderDetail(id) }

    suspend fun cancelOrder(id: Int) = safeApiCall { api.cancelOrder(id) }

    suspend fun payOrder(id: Int) = safeApiCall { api.payOrder(id) }
}

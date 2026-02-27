package com.funTrip.fun2go.data.repository

import com.funTrip.fun2go.data.model.*
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
    suspend fun createItinerary(title: String, start: String, end: String) = safeApiCall {
        api.createItinerary(ItineraryRequest(title, start.ifEmpty { null }, end.ifEmpty { null }))
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

    suspend fun updateItinerary(id: Int, request: ItineraryRequest) = safeApiCall {
        api.updateItinerary(id, request)
    }

    suspend fun deleteItinerary(id: Int) = safeApiCall { api.deleteItinerary(id) }

    suspend fun copyItinerary(id: Int) = safeApiCall { api.copyItinerary(id) }

    suspend fun publishItinerary(id: Int) = safeApiCall { api.publishItinerary(id) }

    // --- Itinerary Days ---

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

    suspend fun createSpot(spot: Spot) = safeApiCall { api.createSpot(spot) }

    // --- Favorites ---

    suspend fun addFavorite(userId: Int, spotId: Int) = safeApiCall {
        api.addFavorite(FavoriteRequest(userId, spotId))
    }

    suspend fun removeFavorite(spotId: Int, userId: Int) = safeApiCall {
        api.removeFavorite(spotId, UnfavoriteRequest(userId))
    }
}

package com.funTrip.fun2go.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.funTrip.fun2go.data.model.*
import com.funTrip.fun2go.data.remote.NetworkResult
import com.funTrip.fun2go.data.repository.TripRepository
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    private val repository = TripRepository()

    // --- 用戶 ---
    private val _userResponse = MutableLiveData<NetworkResult<User>>()
    val userResponse: LiveData<NetworkResult<User>> = _userResponse

    private val _createUserResponse = MutableLiveData<NetworkResult<User>>()
    val createUserResponse: LiveData<NetworkResult<User>> = _createUserResponse

    private val _updateUserResponse = MutableLiveData<NetworkResult<User>>()
    val updateUserResponse: LiveData<NetworkResult<User>> = _updateUserResponse

    private val _userItinerariesResponse = MutableLiveData<NetworkResult<List<Itinerary>>>()
    val userItinerariesResponse: LiveData<NetworkResult<List<Itinerary>>> = _userItinerariesResponse

    private val _userFavoritesResponse = MutableLiveData<NetworkResult<List<Spot>>>()
    val userFavoritesResponse: LiveData<NetworkResult<List<Spot>>> = _userFavoritesResponse

    // --- 景點搜尋 ---
    private val _spotsResponse = MutableLiveData<NetworkResult<List<Spot>>>()
    val spotsResponse: LiveData<NetworkResult<List<Spot>>> = _spotsResponse

    // --- 行程 ---
    private val _itineraryDetail = MutableLiveData<NetworkResult<Itinerary>>()
    val itineraryDetail: LiveData<NetworkResult<Itinerary>> = _itineraryDetail

    private val _itinerariesResponse = MutableLiveData<NetworkResult<List<Itinerary>>>()
    val itinerariesResponse: LiveData<NetworkResult<List<Itinerary>>> = _itinerariesResponse

    private val _createItineraryResponse = MutableLiveData<NetworkResult<Itinerary>>()
    val createItineraryResponse: LiveData<NetworkResult<Itinerary>> = _createItineraryResponse

    private val _updateItineraryResponse = MutableLiveData<NetworkResult<Itinerary>>()
    val updateItineraryResponse: LiveData<NetworkResult<Itinerary>> = _updateItineraryResponse

    private val _deleteItineraryResponse = MutableLiveData<NetworkResult<Unit>>()
    val deleteItineraryResponse: LiveData<NetworkResult<Unit>> = _deleteItineraryResponse

    private val _copyItineraryResponse = MutableLiveData<NetworkResult<Itinerary>>()
    val copyItineraryResponse: LiveData<NetworkResult<Itinerary>> = _copyItineraryResponse

    private val _publishItineraryResponse = MutableLiveData<NetworkResult<Itinerary>>()
    val publishItineraryResponse: LiveData<NetworkResult<Itinerary>> = _publishItineraryResponse

    // --- 行程天數 ---
    private val _addDayResponse = MutableLiveData<NetworkResult<ItineraryDay>>()
    val addDayResponse: LiveData<NetworkResult<ItineraryDay>> = _addDayResponse

    private val _updateDayResponse = MutableLiveData<NetworkResult<ItineraryDay>>()
    val updateDayResponse: LiveData<NetworkResult<ItineraryDay>> = _updateDayResponse

    private val _deleteDayResponse = MutableLiveData<NetworkResult<Unit>>()
    val deleteDayResponse: LiveData<NetworkResult<Unit>> = _deleteDayResponse

    // --- 行程景點 ---
    private val _addSpotToDayResponse = MutableLiveData<NetworkResult<ItinerarySpot>>()
    val addSpotToDayResponse: LiveData<NetworkResult<ItinerarySpot>> = _addSpotToDayResponse

    private val _updateSpotInDayResponse = MutableLiveData<NetworkResult<ItinerarySpot>>()
    val updateSpotInDayResponse: LiveData<NetworkResult<ItinerarySpot>> = _updateSpotInDayResponse

    private val _removeSpotResponse = MutableLiveData<NetworkResult<Unit>>()
    val removeSpotResponse: LiveData<NetworkResult<Unit>> = _removeSpotResponse

    private val _reorderSpotsResponse = MutableLiveData<NetworkResult<Unit>>()
    val reorderSpotsResponse: LiveData<NetworkResult<Unit>> = _reorderSpotsResponse

    // --- 景點 ---
    private val _spotDetailResponse = MutableLiveData<NetworkResult<Spot>>()
    val spotDetailResponse: LiveData<NetworkResult<Spot>> = _spotDetailResponse

    private val _createSpotResponse = MutableLiveData<NetworkResult<Spot>>()
    val createSpotResponse: LiveData<NetworkResult<Spot>> = _createSpotResponse

    // --- 收藏 ---
    private val _favoriteResponse = MutableLiveData<NetworkResult<Unit>>()
    val favoriteResponse: LiveData<NetworkResult<Unit>> = _favoriteResponse

    private val _unfavoriteResponse = MutableLiveData<NetworkResult<Unit>>()
    val unfavoriteResponse: LiveData<NetworkResult<Unit>> = _unfavoriteResponse

    // ========== Functions ==========

    fun fetchUser(userId: Int) {
        _userResponse.value = NetworkResult.Loading()
        viewModelScope.launch {
            _userResponse.value = repository.getUser(userId)
        }
    }

    fun searchSpots(keyword: String) {
        _spotsResponse.value = NetworkResult.Loading()
        viewModelScope.launch {
            _spotsResponse.value = repository.searchSpots(keyword)
        }
    }

    fun fetchAllSpots() {
        _spotsResponse.value = NetworkResult.Loading()
        viewModelScope.launch {
            _spotsResponse.value = repository.getAllSpots()
        }
    }

    fun fetchItineraryDetail(id: Int) {
        _itineraryDetail.value = NetworkResult.Loading()
        viewModelScope.launch {
            _itineraryDetail.value = repository.getItineraryDetail(id)
        }
    }

    fun createUser(name: String, email: String?) {
        _createUserResponse.value = NetworkResult.Loading()
        viewModelScope.launch {
            _createUserResponse.value = repository.createUser(name, email)
        }
    }

    fun updateUser(id: Int, name: String, email: String?) {
        _updateUserResponse.value = NetworkResult.Loading()
        viewModelScope.launch {
            _updateUserResponse.value = repository.updateUser(id, name, email)
        }
    }

    fun fetchUserItineraries(userId: Int) {
        _userItinerariesResponse.value = NetworkResult.Loading()
        viewModelScope.launch {
            _userItinerariesResponse.value = repository.getUserItineraries(userId)
        }
    }

    fun fetchUserFavorites(userId: Int) {
        _userFavoritesResponse.value = NetworkResult.Loading()
        viewModelScope.launch {
            _userFavoritesResponse.value = repository.getUserFavorites(userId)
        }
    }

    fun fetchItineraries(limit: Int = 20, offset: Int = 0) {
        _itinerariesResponse.value = NetworkResult.Loading()
        viewModelScope.launch {
            _itinerariesResponse.value = repository.getItineraries(limit, offset)
        }
    }

    fun createItinerary(title: String, start: String, end: String) {
        _createItineraryResponse.value = NetworkResult.Loading()
        viewModelScope.launch {
            _createItineraryResponse.value = repository.createItinerary(title, start, end)
        }
    }

    fun updateItinerary(id: Int, request: ItineraryRequest) {
        _updateItineraryResponse.value = NetworkResult.Loading()
        viewModelScope.launch {
            _updateItineraryResponse.value = repository.updateItinerary(id, request)
        }
    }

    fun deleteItinerary(id: Int) {
        _deleteItineraryResponse.value = NetworkResult.Loading()
        viewModelScope.launch {
            _deleteItineraryResponse.value = repository.deleteItinerary(id)
        }
    }

    fun copyItinerary(id: Int) {
        _copyItineraryResponse.value = NetworkResult.Loading()
        viewModelScope.launch {
            _copyItineraryResponse.value = repository.copyItinerary(id)
        }
    }

    fun publishItinerary(id: Int) {
        _publishItineraryResponse.value = NetworkResult.Loading()
        viewModelScope.launch {
            _publishItineraryResponse.value = repository.publishItinerary(id)
        }
    }

    fun addDay(itineraryId: Int) {
        _addDayResponse.value = NetworkResult.Loading()
        viewModelScope.launch {
            _addDayResponse.value = repository.addDay(itineraryId)
        }
    }

    fun updateDay(itineraryId: Int, dayId: Int, data: Map<String, String>) {
        _updateDayResponse.value = NetworkResult.Loading()
        viewModelScope.launch {
            _updateDayResponse.value = repository.updateDay(itineraryId, dayId, data)
        }
    }

    fun deleteDay(itineraryId: Int, dayId: Int) {
        _deleteDayResponse.value = NetworkResult.Loading()
        viewModelScope.launch {
            _deleteDayResponse.value = repository.deleteDay(itineraryId, dayId)
        }
    }

    fun addSpotToDay(dayId: Int, request: AddSpotToDayRequest) {
        _addSpotToDayResponse.value = NetworkResult.Loading()
        viewModelScope.launch {
            _addSpotToDayResponse.value = repository.addSpotToDay(dayId, request)
        }
    }

    fun updateSpotInDay(dayId: Int, spotId: Int, request: AddSpotToDayRequest) {
        _updateSpotInDayResponse.value = NetworkResult.Loading()
        viewModelScope.launch {
            _updateSpotInDayResponse.value = repository.updateSpotInDay(dayId, spotId, request)
        }
    }

    fun removeSpotFromDay(dayId: Int, spotId: Int) {
        _removeSpotResponse.value = NetworkResult.Loading()
        viewModelScope.launch {
            _removeSpotResponse.value = repository.removeSpotFromDay(dayId, spotId)
        }
    }

    fun reorderSpots(dayId: Int, spotIds: List<Int>) {
        _reorderSpotsResponse.value = NetworkResult.Loading()
        viewModelScope.launch {
            _reorderSpotsResponse.value = repository.reorderSpots(dayId, spotIds)
        }
    }

    fun fetchSpotDetail(id: Int) {
        _spotDetailResponse.value = NetworkResult.Loading()
        viewModelScope.launch {
            _spotDetailResponse.value = repository.getSpotDetail(id)
        }
    }

    fun createSpot(spot: Spot) {
        _createSpotResponse.value = NetworkResult.Loading()
        viewModelScope.launch {
            _createSpotResponse.value = repository.createSpot(spot)
        }
    }

    fun addFavorite(userId: Int, spotId: Int) {
        _favoriteResponse.value = NetworkResult.Loading()
        viewModelScope.launch {
            _favoriteResponse.value = repository.addFavorite(userId, spotId)
        }
    }

    fun removeFavorite(spotId: Int, userId: Int) {
        _unfavoriteResponse.value = NetworkResult.Loading()
        viewModelScope.launch {
            _unfavoriteResponse.value = repository.removeFavorite(spotId, userId)
        }
    }
}

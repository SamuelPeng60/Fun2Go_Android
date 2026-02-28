package com.funTrip.fun2go.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.funTrip.fun2go.data.local.AppDatabase
import com.funTrip.fun2go.data.local.SavedSpotEntity
import com.funTrip.fun2go.data.local.TokenManager
import com.funTrip.fun2go.data.model.AddSpotToDayRequest
import com.funTrip.fun2go.data.model.Itinerary
import com.funTrip.fun2go.data.model.User
import com.funTrip.fun2go.data.remote.NetworkResult
import com.funTrip.fun2go.data.repository.TripRepository
import android.util.Log
import kotlinx.coroutines.launch

class ItineraryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TripRepository()
    private val tokenManager = TokenManager.getInstance(application)

    val currentUser: User? get() = tokenManager.getSavedUser()
    val hasValidToken: Boolean get() = tokenManager.getAccessToken() != null

    // ─── Saved Spots (for spot picker) ────────────────────────────────────
    val savedSpots: LiveData<List<SavedSpotEntity>> =
        AppDatabase.getDatabase(application).savedSpotDao().getAllSavedSpots()

    // ─── Itinerary Detail (used by ItineraryDetailActivity) ───────────────
    private val _itineraryDetail = MutableLiveData<NetworkResult<Itinerary>>()
    val itineraryDetail: LiveData<NetworkResult<Itinerary>> = _itineraryDetail

    private val _deleteResult = MutableLiveData<NetworkResult<Unit>>()
    val deleteResult: LiveData<NetworkResult<Unit>> = _deleteResult

    private val _addDayResult = MutableLiveData<NetworkResult<Unit>>()
    val addDayResult: LiveData<NetworkResult<Unit>> = _addDayResult

    private val _spotOperationResult = MutableLiveData<NetworkResult<Unit>>()
    val spotOperationResult: LiveData<NetworkResult<Unit>> = _spotOperationResult

    fun loadItinerary(id: Int) {
        _itineraryDetail.value = NetworkResult.Loading()
        viewModelScope.launch {
            _itineraryDetail.value = repository.getItineraryDetail(id)
        }
    }

    fun deleteItinerary(id: Int) {
        _deleteResult.value = NetworkResult.Loading()
        viewModelScope.launch {
            _deleteResult.value = repository.deleteItinerary(id)
        }
    }

    fun addDay(itineraryId: Int) {
        _addDayResult.value = NetworkResult.Loading()
        viewModelScope.launch {
            val currentDayCount = (_itineraryDetail.value as? NetworkResult.Success)?.data?.days?.size ?: 0
            val result = repository.addDay(itineraryId, currentDayCount + 1)
            if (result is NetworkResult.Success) {
                loadItinerary(itineraryId)
                _addDayResult.value = NetworkResult.Success(Unit)
            } else {
                _addDayResult.value = NetworkResult.Error(
                    (result as? NetworkResult.Error)?.message ?: "新增天數失敗"
                )
            }
        }
    }

    fun addSpotToDay(itineraryId: Int, dayId: Int, spotId: Int, orderIndex: Int) {
        _spotOperationResult.value = NetworkResult.Loading()
        viewModelScope.launch {
            val result = repository.addSpotToDay(
                dayId,
                AddSpotToDayRequest(spotId, orderIndex, null, null)
            )
            if (result is NetworkResult.Success) {
                loadItinerary(itineraryId)
                _spotOperationResult.value = NetworkResult.Success(Unit)
            } else {
                _spotOperationResult.value = NetworkResult.Error(
                    (result as? NetworkResult.Error)?.message ?: "新增景點失敗"
                )
            }
        }
    }

    fun removeSpotFromDay(itineraryId: Int, dayId: Int, itinerarySpotId: Int) {
        _spotOperationResult.value = NetworkResult.Loading()
        viewModelScope.launch {
            val result = repository.removeSpotFromDay(dayId, itinerarySpotId)
            if (result is NetworkResult.Success) {
                loadItinerary(itineraryId)
                _spotOperationResult.value = NetworkResult.Success(Unit)
            } else {
                _spotOperationResult.value = NetworkResult.Error(
                    (result as? NetworkResult.Error)?.message ?: "移除景點失敗"
                )
            }
        }
    }

    // ─── User Itinerary List (used by ItineraryListActivity) ──────────────
    private val _userItineraries = MutableLiveData<NetworkResult<List<Itinerary>>>()
    val userItineraries: LiveData<NetworkResult<List<Itinerary>>> = _userItineraries

    private val _createResult = MutableLiveData<NetworkResult<Itinerary>>()
    val createResult: LiveData<NetworkResult<Itinerary>> = _createResult

    private val _updateResult = MutableLiveData<NetworkResult<Itinerary>>()
    val updateResult: LiveData<NetworkResult<Itinerary>> = _updateResult

    fun fetchUserItineraries(userId: Int) {
        Log.d("ILA_DEBUG", "ViewModel.fetchUserItineraries: userId=$userId")
        _userItineraries.value = NetworkResult.Loading()
        viewModelScope.launch {
            val result = repository.getUserItineraries(userId)
            Log.d("ILA_DEBUG", "ViewModel.fetchUserItineraries result: ${result::class.simpleName}" +
                    if (result is NetworkResult.Success) ", count=${result.data?.size}" else ", msg=${(result as? NetworkResult.Error)?.message}")
            _userItineraries.value = result
        }
    }

    fun createItinerary(title: String, totalDays: Int?, destination: String?) {
        _createResult.value = NetworkResult.Loading()
        viewModelScope.launch {
            _createResult.value = repository.createItinerary(title, totalDays, destination)
        }
    }

    fun updateItinerary(id: Int, title: String, totalDays: Int?, destination: String?) {
        _updateResult.value = NetworkResult.Loading()
        viewModelScope.launch {
            _updateResult.value = repository.updateItinerary(id, title, totalDays, destination)
        }
    }
}

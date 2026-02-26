package com.funTrip.fun2go.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.funTrip.fun2go.data.local.TokenManager
import com.funTrip.fun2go.data.model.Itinerary
import com.funTrip.fun2go.data.model.ItineraryRequest
import com.funTrip.fun2go.data.model.User
import com.funTrip.fun2go.data.remote.NetworkResult
import com.funTrip.fun2go.data.repository.TripRepository
import kotlinx.coroutines.launch

class ItineraryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TripRepository()
    private val tokenManager = TokenManager.getInstance(application)

    val currentUser: User? get() = tokenManager.getSavedUser()

    // ─── Itinerary Detail (used by ItineraryDetailActivity) ───
    private val _itineraryDetail = MutableLiveData<NetworkResult<Itinerary>>()
    val itineraryDetail: LiveData<NetworkResult<Itinerary>> = _itineraryDetail

    private val _deleteResult = MutableLiveData<NetworkResult<Unit>>()
    val deleteResult: LiveData<NetworkResult<Unit>> = _deleteResult

    private val _addDayResult = MutableLiveData<NetworkResult<Unit>>()
    val addDayResult: LiveData<NetworkResult<Unit>> = _addDayResult

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

    // ─── User Itinerary List (used by ItineraryListActivity) ──
    private val _userItineraries = MutableLiveData<NetworkResult<List<Itinerary>>>()
    val userItineraries: LiveData<NetworkResult<List<Itinerary>>> = _userItineraries

    private val _createResult = MutableLiveData<NetworkResult<Itinerary>>()
    val createResult: LiveData<NetworkResult<Itinerary>> = _createResult

    private val _updateResult = MutableLiveData<NetworkResult<Itinerary>>()
    val updateResult: LiveData<NetworkResult<Itinerary>> = _updateResult

    fun fetchUserItineraries(userId: Int) {
        _userItineraries.value = NetworkResult.Loading()
        viewModelScope.launch {
            _userItineraries.value = repository.getUserItineraries(userId)
        }
    }

    fun createItinerary(title: String, start: String, end: String) {
        _createResult.value = NetworkResult.Loading()
        viewModelScope.launch {
            _createResult.value = repository.createItinerary(title, start, end)
        }
    }

    fun updateItinerary(id: Int, title: String, start: String, end: String) {
        _updateResult.value = NetworkResult.Loading()
        viewModelScope.launch {
            _updateResult.value = repository.updateItinerary(
                id,
                ItineraryRequest(title, start.ifEmpty { null }, end.ifEmpty { null })
            )
        }
    }
}

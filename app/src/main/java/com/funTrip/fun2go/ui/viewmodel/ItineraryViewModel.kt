package com.funTrip.fun2go.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.funTrip.fun2go.data.model.Itinerary
import com.funTrip.fun2go.data.remote.NetworkResult
import com.funTrip.fun2go.data.repository.TripRepository
import kotlinx.coroutines.launch

class ItineraryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TripRepository()

    private val _itineraryDetail = MutableLiveData<NetworkResult<Itinerary>>()
    val itineraryDetail: LiveData<NetworkResult<Itinerary>> = _itineraryDetail

    private val _deleteResult = MutableLiveData<NetworkResult<Unit>>()
    val deleteResult: LiveData<NetworkResult<Unit>> = _deleteResult

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
}

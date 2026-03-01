package com.funTrip.fun2go.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.funTrip.fun2go.data.model.Vehicle
import com.funTrip.fun2go.data.remote.NetworkResult
import com.funTrip.fun2go.data.repository.TripRepository
import kotlinx.coroutines.launch

class VehicleViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = TripRepository()

    private val _vehicles = MutableLiveData<NetworkResult<List<Vehicle>>>()
    val vehicles: LiveData<NetworkResult<List<Vehicle>>> = _vehicles

    fun fetchVehicles(type: String? = null, available: Boolean? = null) {
        _vehicles.value = NetworkResult.Loading()
        viewModelScope.launch {
            _vehicles.value = repo.getVehicles(type, available)
        }
    }
}

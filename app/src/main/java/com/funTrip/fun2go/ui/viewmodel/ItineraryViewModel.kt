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
import com.funTrip.fun2go.data.model.UploadResponse
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

    private var lastItineraryId: Int = -1

    // ─── Itinerary Detail (used by ItineraryDetailActivity) ───────────────
    private val _itineraryDetail = MutableLiveData<NetworkResult<Itinerary>>()
    val itineraryDetail: LiveData<NetworkResult<Itinerary>> = _itineraryDetail

    private val _deleteResult = MutableLiveData<NetworkResult<Unit>>()
    val deleteResult: LiveData<NetworkResult<Unit>> = _deleteResult

    private val _addDayResult = MutableLiveData<NetworkResult<Unit>>()
    val addDayResult: LiveData<NetworkResult<Unit>> = _addDayResult

    private val _initDaysResult = MutableLiveData<NetworkResult<Unit>>()
    val initDaysResult: LiveData<NetworkResult<Unit>> = _initDaysResult

    fun initItineraryDays(itineraryId: Int, totalDays: Int, startDate: String?) {
        _initDaysResult.value = NetworkResult.Loading()
        viewModelScope.launch {
            _initDaysResult.value = repository.initItineraryDays(itineraryId, totalDays, startDate)
        }
    }

    private val _updateDayResult = MutableLiveData<NetworkResult<Unit>>()
    val updateDayResult: LiveData<NetworkResult<Unit>> = _updateDayResult

    private val _spotOperationResult = MutableLiveData<NetworkResult<Unit>>()
    val spotOperationResult: LiveData<NetworkResult<Unit>> = _spotOperationResult

    fun loadItinerary(id: Int) {
        lastItineraryId = id
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

    fun updateDayDate(itineraryId: Int, dayId: Int, date: String) {
        viewModelScope.launch {
            val result = repository.updateDay(itineraryId, dayId, mapOf("date" to date))
            if (result is NetworkResult.Success) {
                loadItinerary(itineraryId)
                _updateDayResult.value = NetworkResult.Success(Unit)
            } else {
                _updateDayResult.value = NetworkResult.Error(
                    (result as? NetworkResult.Error)?.message ?: "更新日期失敗"
                )
            }
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

    private val _reorderResult = MutableLiveData<NetworkResult<Unit>>()
    val reorderResult: LiveData<NetworkResult<Unit>> = _reorderResult

    fun reorderSpots(dayId: Int, spotIds: List<Int>) {
        _reorderResult.value = NetworkResult.Loading()
        viewModelScope.launch {
            val result = repository.reorderSpots(dayId, spotIds)
            // 失敗時重載以還原伺服器上的真實順序；成功時本地 rebuildList() 已即時刷新距離
            if (result is NetworkResult.Error && lastItineraryId != -1)
                loadItinerary(lastItineraryId)
            _reorderResult.value = when (result) {
                is NetworkResult.Success -> NetworkResult.Success(Unit)
                is NetworkResult.Error   -> NetworkResult.Error(result.message ?: "排序失敗")
                else -> NetworkResult.Error("排序失敗")
            }
        }
    }

    // ─── Public Itinerary List ────────────────────────────────────────────
    private val _publicItineraries = MutableLiveData<NetworkResult<List<Itinerary>>>()
    val publicItineraries: LiveData<NetworkResult<List<Itinerary>>> = _publicItineraries

    fun fetchPublicItineraries() {
        _publicItineraries.value = NetworkResult.Loading()
        viewModelScope.launch { _publicItineraries.value = repository.getItineraries(20, 0) }
    }

    // ─── 行程複製 ─────────────────────────────────────────────────────────
    private val _copyResult = MutableLiveData<NetworkResult<Itinerary>>()
    val copyResult: LiveData<NetworkResult<Itinerary>> = _copyResult

    fun copyItinerary(id: Int) {
        _copyResult.value = NetworkResult.Loading()
        viewModelScope.launch { _copyResult.value = repository.copyItinerary(id) }
    }

    /** 複製行程後，依 startDate 更新各天日期（天數已由後端複製好，只需 PATCH date） */
    fun setDatesAfterCopy(itineraryId: Int, startDate: String) {
        _initDaysResult.value = NetworkResult.Loading()
        viewModelScope.launch {
            val detail = repository.getItineraryDetail(itineraryId)
            if (detail is NetworkResult.Error) {
                _initDaysResult.value = NetworkResult.Error(detail.message ?: "")
                return@launch
            }
            val days = (detail as NetworkResult.Success).data?.days ?: emptyList()
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            val startCal = runCatching {
                java.util.Calendar.getInstance().also { it.time = sdf.parse(startDate)!! }
            }.getOrNull()
            if (startCal != null) {
                for (day in days.sortedBy { it.day_number }) {
                    val cal = java.util.Calendar.getInstance().also { it.time = startCal.time }
                    cal.add(java.util.Calendar.DAY_OF_MONTH, day.day_number - 1)
                    repository.updateDay(itineraryId, day.id, mapOf("date" to sdf.format(cal.time)))
                }
            }
            _initDaysResult.value = NetworkResult.Success(Unit)
        }
    }

    // ─── Publish / Unpublish ──────────────────────────────────────────────
    private val _publishResult = MutableLiveData<NetworkResult<Itinerary>>()
    val publishResult: LiveData<NetworkResult<Itinerary>> = _publishResult

    fun publishItinerary(id: Int) {
        _publishResult.value = NetworkResult.Loading()
        viewModelScope.launch { _publishResult.value = repository.publishItinerary(id) }
    }

    fun unpublishItinerary(itinerary: Itinerary) {
        _publishResult.value = NetworkResult.Loading()
        viewModelScope.launch {
            _publishResult.value = repository.updateItinerary(
                itinerary.id, itinerary.title, itinerary.total_days,
                itinerary.destination, itinerary.coverImageUrl, isPublic = false
            )
        }
    }

    // ─── Image Upload ─────────────────────────────────────────────────────
    private val _uploadImageResult = MutableLiveData<NetworkResult<UploadResponse>>()
    val uploadImageResult: LiveData<NetworkResult<UploadResponse>> = _uploadImageResult

    fun uploadImage(folder: String, bytes: ByteArray, mimeType: String) {
        _uploadImageResult.value = NetworkResult.Loading()
        viewModelScope.launch {
            _uploadImageResult.value = repository.uploadImage(folder, bytes, mimeType)
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

    fun createItinerary(title: String, totalDays: Int?, destination: String?, coverImageUrl: String? = null) {
        _createResult.value = NetworkResult.Loading()
        viewModelScope.launch {
            _createResult.value = repository.createItinerary(title, totalDays, destination, coverImageUrl)
        }
    }

    fun updateItinerary(id: Int, title: String, totalDays: Int?, destination: String?, coverImageUrl: String? = null) {
        _updateResult.value = NetworkResult.Loading()
        viewModelScope.launch {
            _updateResult.value = repository.updateItinerary(id, title, totalDays, destination, coverImageUrl)
        }
    }
}

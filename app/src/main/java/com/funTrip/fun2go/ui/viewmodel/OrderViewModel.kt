package com.funTrip.fun2go.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.funTrip.fun2go.data.local.TokenManager
import com.funTrip.fun2go.data.model.CreateOrderRequest
import com.funTrip.fun2go.data.model.Order
import com.funTrip.fun2go.data.model.Payment
import com.funTrip.fun2go.data.model.User
import com.funTrip.fun2go.data.remote.NetworkResult
import com.funTrip.fun2go.data.repository.TripRepository
import kotlinx.coroutines.launch

class OrderViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = TripRepository()
    private val tokenManager = TokenManager.getInstance(app)

    val currentUser: User? get() = tokenManager.getSavedUser()

    private val _createOrderResult = MutableLiveData<NetworkResult<Order>>()
    val createOrderResult: LiveData<NetworkResult<Order>> = _createOrderResult

    private val _orders = MutableLiveData<NetworkResult<List<Order>>>()
    val orders: LiveData<NetworkResult<List<Order>>> = _orders

    private val _orderDetail = MutableLiveData<NetworkResult<Order>>()
    val orderDetail: LiveData<NetworkResult<Order>> = _orderDetail

    private val _cancelResult = MutableLiveData<NetworkResult<Order>>()
    val cancelResult: LiveData<NetworkResult<Order>> = _cancelResult

    private val _payResult = MutableLiveData<NetworkResult<Payment>>()
    val payResult: LiveData<NetworkResult<Payment>> = _payResult

    fun createOrder(req: CreateOrderRequest) {
        _createOrderResult.value = NetworkResult.Loading()
        viewModelScope.launch {
            _createOrderResult.value = repo.createOrder(req)
        }
    }

    fun fetchOrders(status: String? = null) {
        _orders.value = NetworkResult.Loading()
        viewModelScope.launch {
            _orders.value = repo.getOrders(status)
        }
    }

    fun fetchOrderDetail(id: Int) {
        _orderDetail.value = NetworkResult.Loading()
        viewModelScope.launch {
            _orderDetail.value = repo.getOrderDetail(id)
        }
    }

    fun cancelOrder(id: Int) {
        _cancelResult.value = NetworkResult.Loading()
        viewModelScope.launch {
            _cancelResult.value = repo.cancelOrder(id)
        }
    }

    fun payOrder(id: Int) {
        _payResult.value = NetworkResult.Loading()
        viewModelScope.launch {
            _payResult.value = repo.payOrder(id)
        }
    }
}

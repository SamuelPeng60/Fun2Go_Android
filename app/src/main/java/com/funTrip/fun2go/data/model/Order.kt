package com.funTrip.fun2go.data.model

import com.google.gson.annotations.SerializedName

data class Payment(
    val id: Int,
    @SerializedName("order_id")    val orderId: Int,
    val amount: String,
    val method: String,
    val status: String,
    @SerializedName("transaction_id") val transactionId: String?,
    @SerializedName("paid_at")     val paidAt: String?,
    @SerializedName("created_at")  val createdAt: String
)

data class CharterBooking(
    val id: Int,
    @SerializedName("order_id")         val orderId: Int,
    @SerializedName("vehicle_id")       val vehicleId: Int,
    @SerializedName("vehicle_name")     val vehicleName: String?,
    @SerializedName("vehicle_type")     val vehicleType: String?,
    @SerializedName("vehicle_capacity") val vehicleCapacity: Int?,
    @SerializedName("pickup_location")  val pickupLocation: String,
    @SerializedName("dropoff_location") val dropoffLocation: String?,
    @SerializedName("pickup_time")      val pickupTime: String,
    @SerializedName("dropoff_time")     val dropoffTime: String?,
    val days: Int,
    @SerializedName("passenger_count")  val passengerCount: Int,
    @SerializedName("contact_name")     val contactName: String,
    @SerializedName("contact_phone")    val contactPhone: String,
    @SerializedName("special_requests") val specialRequests: String?
)

data class Order(
    val id: Int,
    @SerializedName("user_id")         val userId: Int,
    @SerializedName("itinerary_id")    val itineraryId: Int?,
    @SerializedName("order_type")      val orderType: String,
    val status: String,                // pending / confirmed / completed / cancelled
    @SerializedName("total_amount")    val totalAmount: String,
    val note: String?,
    @SerializedName("created_at")      val createdAt: String,
    @SerializedName("updated_at")      val updatedAt: String?,
    @SerializedName("charter_booking") val charterBooking: CharterBooking?,
    val payments: List<Payment>?
)

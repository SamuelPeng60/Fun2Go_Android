package com.funTrip.fun2go.data.remote

import com.funTrip.fun2go.data.model.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // --- 認證 (Auth) ---
    @POST("api/auth/google")
    suspend fun loginWithGoogle(@Body request: GoogleAuthRequest): Response<AuthResponse>

    @POST("api/auth/refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): Response<RefreshTokenResponse>

    @POST("api/auth/logout")
    suspend fun logout(@Body request: LogoutRequest): Response<Unit>

    // --- 用戶 (Users) ---
    @POST("api/users")
    suspend fun createUser(@Body user: UserRequest): Response<User>

    @GET("api/users/{id}")
    suspend fun getUser(@Path("id") id: Int): Response<User>

    @PUT("api/users/{id}")
    suspend fun updateUser(@Path("id") id: Int, @Body user: UserRequest): Response<User>

    @GET("api/users/{id}/itineraries")
    suspend fun getUserItineraries(@Path("id") id: Int): Response<List<Itinerary>>

    @GET("api/users/{id}/favorites")
    suspend fun getUserFavorites(@Path("id") id: Int): Response<List<Spot>>

    // --- 行程 (Itineraries) ---
    @GET("api/itineraries")
    suspend fun getItineraries(
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0
    ): Response<List<Itinerary>>

    @POST("api/itineraries")
    suspend fun createItinerary(@Body itinerary: ItineraryRequest): Response<Itinerary>

    @GET("api/itineraries/{id}")
    suspend fun getItineraryDetail(@Path("id") id: Int): Response<Itinerary>

    @PUT("api/itineraries/{id}")
    suspend fun updateItinerary(@Path("id") id: Int, @Body itinerary: ItineraryRequest): Response<Itinerary>

    @DELETE("api/itineraries/{id}")
    suspend fun deleteItinerary(@Path("id") id: Int): Response<Unit>

    @POST("api/itineraries/{id}/copy")
    suspend fun copyItinerary(@Path("id") id: Int): Response<Itinerary>

    @POST("api/itineraries/{id}/publish")
    suspend fun publishItinerary(@Path("id") id: Int): Response<Itinerary>

    // --- 行程天數 (Itinerary Days) ---
    @POST("api/itineraries/{id}/days")
    suspend fun addDay(@Path("id") itineraryId: Int, @Body request: AddDayRequest): Response<ItineraryDay>

    @PUT("api/itineraries/{id}/days/{dayId}")
    suspend fun updateDay(@Path("id") itineraryId: Int, @Path("dayId") dayId: Int, @Body dayData: Map<String, String>): Response<ItineraryDay>

    @DELETE("api/itineraries/{id}/days/{dayId}")
    suspend fun deleteDay(@Path("id") itineraryId: Int, @Path("dayId") dayId: Int): Response<Unit>

    // --- 行程景點 (Itinerary Spots) ---
    @POST("api/days/{dayId}/spots")
    suspend fun addSpotToDay(@Path("dayId") dayId: Int, @Body request: AddSpotToDayRequest): Response<ItinerarySpot>

    @PUT("api/days/{dayId}/spots/{spotId}")
    suspend fun updateSpotInDay(@Path("dayId") dayId: Int, @Path("spotId") spotId: Int, @Body request: AddSpotToDayRequest): Response<ItinerarySpot>

    @DELETE("api/days/{dayId}/spots/{spotId}")
    suspend fun removeSpotFromDay(@Path("dayId") dayId: Int, @Path("spotId") spotId: Int): Response<Unit>

    @PUT("api/days/{dayId}/spots/reorder")
    suspend fun reorderSpots(@Path("dayId") dayId: Int, @Body request: ReorderRequest): Response<Unit>

    // --- 景點 (Spots) ---
    @GET("api/spots")
    suspend fun searchSpots(
        @Query("keyword") keyword: String? = null,
        @Query("category") category: String? = null,
        @Query("lat") lat: Double? = null,
        @Query("lng") lng: Double? = null,
        @Query("radius") radius: Int? = null
    ): Response<List<Spot>>

    @GET("api/spots/{id}")
    suspend fun getSpotDetail(@Path("id") id: Int): Response<Spot>

    @POST("api/spots")
    suspend fun createSpot(@Body req: SpotRequest): Response<Spot>

    @PUT("api/spots/{id}")
    suspend fun updateSpot(@Path("id") id: Int, @Body req: SpotRequest): Response<Spot>

    @DELETE("api/spots/{id}")
    suspend fun deleteSpot(@Path("id") id: Int): Response<Unit>

    // --- 車輛 (Vehicles) ---
    @GET("api/vehicles")
    suspend fun getVehicles(
        @Query("type")      type: String? = null,
        @Query("available") available: Boolean? = null
    ): Response<List<Vehicle>>

    @GET("api/vehicles/{id}")
    suspend fun getVehicleDetail(@Path("id") id: Int): Response<Vehicle>

    // --- 訂單 (Orders) ---
    @POST("api/orders")
    suspend fun createOrder(@Body req: CreateOrderRequest): Response<Order>

    @GET("api/orders")
    suspend fun getOrders(@Query("status") status: String? = null): Response<List<Order>>

    @GET("api/orders/{id}")
    suspend fun getOrderDetail(@Path("id") id: Int): Response<Order>

    @POST("api/orders/{id}/cancel")
    suspend fun cancelOrder(@Path("id") id: Int): Response<Order>

    @POST("api/orders/{id}/pay")
    suspend fun payOrder(@Path("id") id: Int): Response<Payment>

    // --- 收藏 (Favorites) ---
    @POST("api/favorites")
    suspend fun addFavorite(@Body request: FavoriteRequest): Response<Unit>

    // 注意: Retrofit 的 DELETE 預設不支援 Body，需使用 HTTP annotation 自定義
    @HTTP(method = "DELETE", path = "api/favorites/{spotId}", hasBody = true)
    suspend fun removeFavorite(@Path("spotId") spotId: Int, @Body request: UnfavoriteRequest): Response<Unit>

    // --- 上傳 (Upload) ---
    @Multipart
    @POST("api/upload")
    suspend fun uploadImage(
        @Part file: MultipartBody.Part,
        @Part("folder") folder: RequestBody
    ): Response<UploadResponse>
}
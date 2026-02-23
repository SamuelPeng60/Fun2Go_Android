package com.funTrip.fun2go.data.remote

import com.funTrip.fun2go.data.model.DistanceMatrixResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface GoogleMapsApiService {

    @GET("maps/api/distancematrix/json")
    suspend fun getDistanceMatrix(
        @Query("origins") origins: String,
        @Query("destinations") destinations: String,
        @Query("mode") mode: String = "driving",
        @Query("language") language: String = "zh-TW",
        @Query("key") key: String
    ): Response<DistanceMatrixResponse>
}

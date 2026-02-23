package com.funTrip.fun2go.data.remote

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object GoogleMapsRetrofitClient {
    private const val BASE_URL = "https://maps.googleapis.com/"

    val apiService: GoogleMapsApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GoogleMapsApiService::class.java)
    }
}

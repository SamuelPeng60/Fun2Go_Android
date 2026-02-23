package com.funTrip.fun2go.data.repository

import com.funTrip.fun2go.data.remote.NetworkResult
import org.json.JSONObject
import retrofit2.Response

abstract class BaseRepository {

    suspend fun <T> safeApiCall(apiCall: suspend () -> Response<T>): NetworkResult<T> {
        try {
            val response = apiCall()
            if (response.isSuccessful) {
                val body = response.body()
                body?.let {
                    return NetworkResult.Success(body)
                }
            }

            // 解析錯誤訊息 { "error": "錯誤訊息" }
            val errorBody = response.errorBody()?.string()
            val errorMessage = try {
                val jsonObject = JSONObject(errorBody)
                jsonObject.getString("error")
            } catch (e: Exception) {
                "Error: ${response.code()} ${response.message()}"
            }

            return NetworkResult.Error(errorMessage)
        } catch (e: Exception) {
            return NetworkResult.Error(e.message ?: "Unknown Error")
        }
    }
}
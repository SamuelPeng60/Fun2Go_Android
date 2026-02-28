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
                if (body != null) {
                    return NetworkResult.Success(body)
                }
                // 204 No Content（或 200 with empty body）：沒有 body 但仍是成功
                // 適用於 DELETE 等回傳 204 的 endpoint（Response<Unit>）
                @Suppress("UNCHECKED_CAST")
                return NetworkResult.Success(Unit as T)
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
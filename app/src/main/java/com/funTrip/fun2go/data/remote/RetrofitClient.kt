package com.funTrip.fun2go.data.remote

import com.funTrip.fun2go.data.local.TokenManager
import com.funTrip.fun2go.data.model.RefreshTokenRequest
import com.funTrip.fun2go.data.model.RefreshTokenResponse
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

object RetrofitClient {
    private const val BASE_URL = "https://v1api.samuelray.net/"

    // 由 MainViewModel 初始化後注入
    var tokenManager: TokenManager? = null

    // 只用來同步呼叫 refresh endpoint（Authenticator 不能用 coroutine）
    private interface RefreshService {
        @POST("api/auth/refresh")
        fun refreshToken(@Body request: RefreshTokenRequest): Call<RefreshTokenResponse>
    }

    private val refreshService: RefreshService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(OkHttpClient())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(RefreshService::class.java)
    }

    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                // 有 token 時自動帶 Authorization header
                val token = tokenManager?.getAccessToken()
                val request = if (token != null) {
                    chain.request().newBuilder()
                        .header("Authorization", "Bearer $token")
                        .build()
                } else {
                    chain.request()
                }
                chain.proceed(request)
            }
            .authenticator { _, response ->
                // 已經重試過一次 → 放棄，避免無限循環
                if (response.priorResponse?.code == 401) return@authenticator null

                val rt = tokenManager?.getRefreshToken() ?: return@authenticator null

                // 同步呼叫 refresh token API
                val refreshResp = try {
                    refreshService.refreshToken(RefreshTokenRequest(rt)).execute()
                } catch (e: Exception) {
                    return@authenticator null
                }

                if (refreshResp.isSuccessful) {
                    val body = refreshResp.body() ?: return@authenticator null
                    tokenManager?.updateTokens(body.accessToken, body.refreshToken)
                    // 用新 token 重試原始請求
                    response.request.newBuilder()
                        .header("Authorization", "Bearer ${body.accessToken}")
                        .build()
                } else {
                    // Refresh token 也失效 → 清除登入狀態
                    tokenManager?.clear()
                    null
                }
            }
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()
    }

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}

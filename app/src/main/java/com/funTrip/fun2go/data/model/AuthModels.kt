package com.funTrip.fun2go.data.model

import com.google.gson.annotations.SerializedName

data class GoogleAuthRequest(
    @SerializedName("id_token") val idToken: String
)

data class AuthResponse(
    val user: User,
    val accessToken: String,
    val refreshToken: String
)

data class RefreshTokenRequest(val refreshToken: String)

data class RefreshTokenResponse(
    val accessToken: String,
    val refreshToken: String
)

data class LogoutRequest(val refreshToken: String)

package com.funTrip.fun2go.data.model

import com.google.gson.annotations.SerializedName

data class User(
    @SerializedName("id")
    val id: Int,
    @SerializedName("name")
    val name: String,
    @SerializedName("email")
    val email: String?,
    @SerializedName("avatar_url")
    val avatarUrl: String? = null,
    @SerializedName("created_at")
    val createdAt: String? = null
)

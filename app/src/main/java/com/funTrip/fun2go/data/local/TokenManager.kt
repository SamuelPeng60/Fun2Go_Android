package com.funTrip.fun2go.data.local

import android.content.Context
import com.funTrip.fun2go.data.model.User
import com.google.gson.Gson

class TokenManager private constructor(context: Context) {

    companion object {
        @Volatile private var instance: TokenManager? = null

        fun getInstance(context: Context): TokenManager =
            instance ?: synchronized(this) {
                instance ?: TokenManager(context.applicationContext).also { instance = it }
            }
    }

    private val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveLoginResult(user: User, accessToken: String, refreshToken: String) {
        prefs.edit()
            .putString("access_token", accessToken)
            .putString("refresh_token", refreshToken)
            .putString("user_json", gson.toJson(user))
            .apply()
    }

    fun updateTokens(accessToken: String, refreshToken: String) {
        prefs.edit()
            .putString("access_token", accessToken)
            .putString("refresh_token", refreshToken)
            .apply()
    }

    fun getAccessToken(): String? = prefs.getString("access_token", null)

    fun getRefreshToken(): String? = prefs.getString("refresh_token", null)

    fun getSavedUser(): User? {
        val json = prefs.getString("user_json", null) ?: return null
        return try { gson.fromJson(json, User::class.java) } catch (e: Exception) { null }
    }

    fun saveGoogleAccount(user: User) {
        // Preserve the existing backend user id (>0) so list fetches still work
        // while loginWithGoogle is in-flight. The real id is confirmed by saveLoginResult().
        val existingId = getSavedUser()?.id ?: 0
        val merged = if (existingId > 0) user.copy(id = existingId) else user
        prefs.edit()
            .putString("user_json", gson.toJson(merged))
            .apply()
    }

    fun isLoggedIn(): Boolean = getSavedUser() != null

    fun clear() = prefs.edit().clear().apply()
}

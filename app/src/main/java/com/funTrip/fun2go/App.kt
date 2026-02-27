package com.funTrip.fun2go

import android.app.Application
import com.funTrip.fun2go.data.local.TokenManager
import com.funTrip.fun2go.data.remote.RetrofitClient

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        // 確保 RetrofitClient 在 App 啟動時就取得 tokenManager，
        // 不依賴任何 ViewModel 的初始化順序（避免 process kill/restore 問題）
        RetrofitClient.tokenManager = TokenManager.getInstance(this)
    }
}

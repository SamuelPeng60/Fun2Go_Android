package com.funTrip.fun2go

import android.app.Application
import com.funTrip.fun2go.data.local.TokenManager
import com.funTrip.fun2go.data.remote.RetrofitClient
import com.google.android.gms.maps.MapsInitializer

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        RetrofitClient.tokenManager = TokenManager.getInstance(this)
        // 在 Application 啟動時就預熱 Maps SDK，
        // 讓 SDK 有更多時間在背景完成初始化，降低 MainActivity 的阻塞時間
        MapsInitializer.initialize(this)
    }
}

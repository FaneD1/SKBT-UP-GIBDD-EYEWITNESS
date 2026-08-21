package com.example.skbt_up_gibdd_eyewitness

import android.app.Application
import com.example.skbt_up_gibdd_eyewitness.app.AppContainer
import com.yandex.mapkit.MapKitFactory

class EyewitnessApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.MAPKIT_API_KEY.isNotBlank()) {
            MapKitFactory.setApiKey(BuildConfig.MAPKIT_API_KEY)
            MapKitFactory.initialize(this)
        }
    }
}

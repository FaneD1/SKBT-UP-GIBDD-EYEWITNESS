package com.example.skbt_up_gibdd_eyewitness

import android.app.Application
import com.example.skbt_up_gibdd_eyewitness.app.AppContainer

class EyewitnessApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}

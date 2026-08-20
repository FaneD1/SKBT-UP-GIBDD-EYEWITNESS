package com.example.skbt_up_gibdd_eyewitness.app

import android.content.Context
import com.example.skbt_up_gibdd_eyewitness.BuildConfig
import com.example.skbt_up_gibdd_eyewitness.core.device.FingerprintProvider
import com.example.skbt_up_gibdd_eyewitness.core.network.NetworkFactory
import com.example.skbt_up_gibdd_eyewitness.core.storage.SecureDeviceStorage
import com.example.skbt_up_gibdd_eyewitness.data.device.DefaultDeviceRepository
import com.example.skbt_up_gibdd_eyewitness.domain.device.DeviceRepository

class AppContainer(context: Context) {
    private val storage = SecureDeviceStorage(context)
    private val fingerprintProvider = FingerprintProvider(context, storage)
    private val deviceApi = NetworkFactory.createDeviceApi(
        baseUrl = BuildConfig.API_BASE_URL,
        sessionProvider = storage::readSession,
        enableHttpLogs = BuildConfig.DEBUG,
    )

    val deviceRepository: DeviceRepository = DefaultDeviceRepository(
        api = deviceApi,
        fingerprintProvider = fingerprintProvider,
        storage = storage,
    )
}

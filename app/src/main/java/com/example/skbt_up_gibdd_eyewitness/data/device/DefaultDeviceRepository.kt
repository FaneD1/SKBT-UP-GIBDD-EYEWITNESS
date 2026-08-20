package com.example.skbt_up_gibdd_eyewitness.data.device

import com.example.skbt_up_gibdd_eyewitness.core.device.FingerprintProvider
import com.example.skbt_up_gibdd_eyewitness.core.network.DeviceApi
import com.example.skbt_up_gibdd_eyewitness.core.network.RegisterDeviceRequest
import com.example.skbt_up_gibdd_eyewitness.core.storage.SecureDeviceStorage
import com.example.skbt_up_gibdd_eyewitness.domain.device.DeviceRepository
import com.example.skbt_up_gibdd_eyewitness.domain.device.DeviceSession

class DefaultDeviceRepository(
    private val api: DeviceApi,
    private val fingerprintProvider: FingerprintProvider,
    private val storage: SecureDeviceStorage,
) : DeviceRepository {
    override suspend fun register(pushToken: String?): Result<DeviceSession> = runCatching {
        val response = api.register(
            RegisterDeviceRequest(
                fingerprintHash = fingerprintProvider.fingerprintHash(),
                pushToken = pushToken,
            ),
        )
        DeviceSession(
            deviceId = response.deviceId,
            accessToken = response.accessToken,
        ).also(storage::saveSession)
    }

    override fun savedSession(): DeviceSession? = storage.readSession()
}

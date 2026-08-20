package com.example.skbt_up_gibdd_eyewitness.domain.device

interface DeviceRepository {
    suspend fun register(pushToken: String? = null): Result<DeviceSession>
    fun savedSession(): DeviceSession?
}

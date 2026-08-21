package com.example.skbt_up_gibdd_eyewitness.domain.message

import android.net.Uri

interface MessageRepository {
    fun currentDeviceId(): String?
    fun currentAccessToken(): String?
    fun mediaDownloadUrl(messageId: String): String
    suspend fun sendText(text: String): Result<ChatMessage>
    suspend fun getOwnMessages(): Result<List<ChatMessage>>
    suspend fun markDelivered(messageId: String): Result<ChatMessage>
    suspend fun sendStaticLocation(latitude: Double, longitude: Double): Result<ChatMessage>
    suspend fun uploadMedia(uri: Uri, mimeType: String, sizeBytes: Long): Result<ChatMessage>
    suspend fun startLiveLocation(): Result<ChatMessage>
    suspend fun sendLiveLocationPoint(messageId: String, latitude: Double, longitude: Double): Result<Unit>
    suspend fun stopLiveLocation(messageId: String): Result<ChatMessage>
}

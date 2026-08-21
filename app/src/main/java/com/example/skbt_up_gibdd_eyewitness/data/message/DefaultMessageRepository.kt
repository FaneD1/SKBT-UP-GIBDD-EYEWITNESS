package com.example.skbt_up_gibdd_eyewitness.data.message

import android.content.ContentResolver
import android.net.Uri
import android.webkit.MimeTypeMap
import com.example.skbt_up_gibdd_eyewitness.core.network.CreateMessageRequest
import com.example.skbt_up_gibdd_eyewitness.core.network.LocationPointRequest
import com.example.skbt_up_gibdd_eyewitness.core.network.MessageApi
import com.example.skbt_up_gibdd_eyewitness.core.network.MessageResponse
import com.example.skbt_up_gibdd_eyewitness.core.storage.SecureDeviceStorage
import com.example.skbt_up_gibdd_eyewitness.domain.message.ChatMessage
import com.example.skbt_up_gibdd_eyewitness.domain.message.MessageRepository
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okio.BufferedSink
import java.io.IOException

class DefaultMessageRepository(
    private val api: MessageApi,
    private val storage: SecureDeviceStorage,
    private val contentResolver: ContentResolver,
    private val baseUrl: String,
) : MessageRepository {
    override fun currentDeviceId(): String? = storage.readSession()?.deviceId
    override fun currentAccessToken(): String? = storage.readSession()?.accessToken
    override fun mediaDownloadUrl(messageId: String): String =
        "${baseUrl}api/v1/messages/$messageId/media"

    override suspend fun sendText(text: String): Result<ChatMessage> =
        runCatching { api.sendText(CreateMessageRequest(text = text)).toDomain() }

    override suspend fun getOwnMessages(): Result<List<ChatMessage>> = runCatching {
        val deviceId = requireNotNull(currentDeviceId()) { "Устройство не зарегистрировано" }
        api.getMessages(deviceId).messages.map { it.toDomain() }
    }

    override suspend fun markDelivered(messageId: String): Result<ChatMessage> =
        runCatching { api.markDelivered(messageId).toDomain() }

    override suspend fun sendStaticLocation(latitude: Double, longitude: Double): Result<ChatMessage> =
        runCatching { api.sendStaticLocation(LocationPointRequest(latitude, longitude)).toDomain() }

    override suspend fun uploadMedia(uri: Uri, mimeType: String, sizeBytes: Long): Result<ChatMessage> = runCatching {
        val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "bin"
        val body = ContentUriRequestBody(contentResolver, uri, mimeType, sizeBytes)
        val part = MultipartBody.Part.createFormData("file", "upload.$extension", body)
        api.uploadMedia(part).toDomain()
    }

    override suspend fun startLiveLocation(): Result<ChatMessage> =
        runCatching { api.startLiveLocation().toDomain() }

    override suspend fun sendLiveLocationPoint(
        messageId: String,
        latitude: Double,
        longitude: Double,
    ): Result<Unit> = runCatching {
        api.sendLiveLocationPoint(messageId, LocationPointRequest(latitude, longitude))
        Unit
    }

    override suspend fun stopLiveLocation(messageId: String): Result<ChatMessage> =
        runCatching { api.stopLiveLocation(messageId).toDomain() }

    private fun MessageResponse.toDomain() = ChatMessage(
        id = messageId,
        observerDeviceId = observerDeviceId,
        senderDeviceId = senderDeviceId,
        text = text.orEmpty(),
        type = messageType,
        staticLatitude = staticLocation?.latitude,
        staticLongitude = staticLocation?.longitude,
        mediaStorageKey = media?.storageKey,
        mediaMimeType = media?.mimeType,
        liveEndsAt = liveLocation?.endsAt,
        createdAt = createdAt,
        deliveredAt = deliveredAt,
    )
}

private class ContentUriRequestBody(
    private val contentResolver: ContentResolver,
    private val uri: Uri,
    private val mimeType: String,
    private val sizeBytes: Long,
) : RequestBody() {
    override fun contentType() = mimeType.toMediaType()
    override fun contentLength() = sizeBytes

    override fun writeTo(sink: BufferedSink) {
        val input = contentResolver.openInputStream(uri) ?: throw IOException("Не удалось открыть выбранный файл")
        input.use { source ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val count = source.read(buffer)
                if (count == -1) break
                sink.write(buffer, 0, count)
            }
        }
    }
}

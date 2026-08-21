package com.example.skbt_up_gibdd_eyewitness.core.network

import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import okhttp3.MultipartBody

interface MessageApi {
    @POST("api/v1/messages")
    suspend fun sendText(@Body request: CreateMessageRequest): MessageResponse

    @GET("api/v1/chats/{observerDeviceId}/messages")
    suspend fun getMessages(
        @Path("observerDeviceId") observerDeviceId: String,
        @Query("after_message_id") afterMessageId: String? = null,
        @Query("limit") limit: Int = 100,
    ): ChatMessagesResponse

    @PATCH("api/v1/messages/{messageId}/delivered")
    suspend fun markDelivered(@Path("messageId") messageId: String): MessageResponse

    @POST("api/v1/messages/static-location")
    suspend fun sendStaticLocation(@Body request: LocationPointRequest): MessageResponse

    @Multipart
    @POST("api/v1/messages/media/upload")
    suspend fun uploadMedia(@Part file: MultipartBody.Part): MessageResponse

    @POST("api/v1/messages/live-location/start")
    suspend fun startLiveLocation(@Body request: EmptyRequest = EmptyRequest()): MessageResponse

    @POST("api/v1/messages/{messageId}/live-location/points")
    suspend fun sendLiveLocationPoint(
        @Path("messageId") messageId: String,
        @Body request: LocationPointRequest,
    ): LiveLocationPointResponse

    @POST("api/v1/messages/{messageId}/live-location/stop")
    suspend fun stopLiveLocation(@Path("messageId") messageId: String): MessageResponse
}

class EmptyRequest

data class LocationPointRequest(val latitude: Double, val longitude: Double)

data class LiveLocationPointResponse(
    @SerializedName("recorded_at") val recordedAt: String,
    val latitude: Double,
    val longitude: Double,
)

data class CreateMessageRequest(
    @SerializedName("message_type") val messageType: String = "TEXT",
    val text: String,
)

data class ChatMessagesResponse(val messages: List<MessageResponse>)

data class MessageResponse(
    @SerializedName("message_id") val messageId: String,
    @SerializedName("observer_device_id") val observerDeviceId: String,
    @SerializedName("sender_device_id") val senderDeviceId: String,
    @SerializedName("message_type") val messageType: String,
    val text: String?,
    @SerializedName("static_location") val staticLocation: StaticLocationResponse?,
    val media: MediaResponse?,
    @SerializedName("live_location") val liveLocation: LiveLocationResponse?,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("delivered_at") val deliveredAt: String?,
)

data class StaticLocationResponse(val latitude: Double, val longitude: Double)

data class MediaResponse(
    @SerializedName("storage_key") val storageKey: String,
    @SerializedName("mime_type") val mimeType: String,
    @SerializedName("last_viewed_at") val lastViewedAt: String?,
)

data class LiveLocationResponse(@SerializedName("ends_at") val endsAt: String)

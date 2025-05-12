package com.haseebali.savelife.data

import android.util.Log
import com.haseebali.savelife.Constants
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class NotificationService {
    private val TAG = "NotificationService"
    private val client = OkHttpClient()

    /**
     * Send a notification to a specific user by their OneSignal external user ID
     * (which should be the same as their Firebase UID)
     */
    fun sendNotificationToUser(
        recipientUserId: String,
        title: String,
        message: String,
        data: Map<String, String> = emptyMap()
    ) {
        val jsonPayload = createNotificationPayload(recipientUserId, title, message, data)
        sendNotification(jsonPayload)
    }

    /**
     * Create the JSON payload for OneSignal REST API
     */
    private fun createNotificationPayload(
        recipientUserId: String,
        title: String,
        message: String,
        data: Map<String, String>
    ): String {
        val json = JSONObject()

        // ✅ REQUIRED FIELD
        json.put("app_id", Constants.ONESIGNAL_APP_ID)

        // Target specific user
        json.put("include_external_user_ids", JSONArray().put(recipientUserId))
        json.put("channel_for_external_user_ids", "push")

        // Content
        json.put("headings", JSONObject().put("en", title))
        json.put("contents", JSONObject().put("en", message))

        // Optional custom data
        if (data.isNotEmpty()) {
            val customData = JSONObject()
            for ((key, value) in data) {
                customData.put(key, value)
            }
            json.put("data", customData)
        }

        return json.toString()
    }

    /**
     * Send the notification using the OneSignal REST API
     */
    private fun sendNotification(jsonPayload: String) {
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = jsonPayload.toRequestBody(mediaType)

        val request = Request.Builder()
            .url("https://onesignal.com/api/v1/notifications")
            .post(requestBody)
            .addHeader("accept", "application/json")
            .addHeader("Authorization", "Basic ${Constants.ONESIGNAL_API_KEY}")
            .addHeader("content-type", "application/json")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Failed to send notification: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    Log.d(TAG, "Notification sent successfully")
                } else {
                    Log.e(TAG, "Error sending notification: ${response.code} - ${response.body?.string()}")
                }
                response.close()
            }
        })
    }
}

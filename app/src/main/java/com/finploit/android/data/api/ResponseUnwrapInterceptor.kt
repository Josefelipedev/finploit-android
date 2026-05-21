package com.finploit.android.data.api

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.json.JSONObject

class ResponseUnwrapInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())

        if (!response.isSuccessful) return response

        val body = response.body ?: return response
        val contentType = body.contentType()
        val bodyString = body.string()

        if (bodyString.isBlank()) {
            return response.newBuilder()
                .body("null".toResponseBody("application/json".toMediaType()))
                .build()
        }

        return try {
            val json = JSONObject(bodyString)
            if (json.has("success") && json.has("data") && !json.isNull("data")) {
                val unwrapped = json.get("data").toString()
                    .toResponseBody("application/json".toMediaType())
                response.newBuilder().body(unwrapped).build()
            } else {
                response.newBuilder()
                    .body(bodyString.toResponseBody(contentType))
                    .build()
            }
        } catch (e: Exception) {
            response.newBuilder()
                .body(bodyString.toResponseBody(contentType))
                .build()
        }
    }
}

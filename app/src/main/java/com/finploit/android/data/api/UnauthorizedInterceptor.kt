package com.finploit.android.data.api

import com.finploit.android.data.local.TokenStorage
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class UnauthorizedInterceptor @Inject constructor(
    private val tokenStorage: TokenStorage,
    private val unauthorizedEventBus: UnauthorizedEventBus,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)
        if (response.code == 401) {
            // Compare the token that was sent with the one currently stored.
            // If they differ, the user already re-logged in with a new token, so this
            // 401 belongs to a stale request from the previous session — ignore it.
            val sentToken = request.header("Authorization")
            val currentToken = tokenStorage.getToken()?.let { "Bearer $it" }
            if (sentToken != null && sentToken == currentToken) {
                tokenStorage.clearToken()
                unauthorizedEventBus.send()
            }
        }
        return response
    }
}

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
        val response = chain.proceed(chain.request())
        if (response.code == 401) {
            tokenStorage.clearToken()
            // Notify the app so it can navigate back to Login
            unauthorizedEventBus.send()
        }
        return response
    }
}

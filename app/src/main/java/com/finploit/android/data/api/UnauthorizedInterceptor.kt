package com.finploit.android.data.api

import com.finploit.android.data.local.TokenStorage
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class UnauthorizedInterceptor @Inject constructor(
    private val tokenStorage: TokenStorage,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        if (response.code == 401) {
            tokenStorage.clearToken()
        }
        return response
    }
}

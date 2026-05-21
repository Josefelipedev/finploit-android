package com.finploit.android.data.repository

import com.finploit.android.data.api.AuthApi
import com.finploit.android.data.dto.AuthResponse
import com.finploit.android.data.dto.GoogleLoginRequest
import com.finploit.android.data.dto.GoogleUserInfo
import com.finploit.android.data.dto.LoginRequest
import com.finploit.android.data.dto.RegisterRequest
import com.finploit.android.data.dto.UserDto
import com.finploit.android.data.local.TokenStorage
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val api: AuthApi,
    private val tokenStorage: TokenStorage,
) {
    suspend fun login(email: String, password: String): Result<AuthResponse> = runCatching {
        val response = api.login(LoginRequest(email, password))
        tokenStorage.saveToken(response.token)
        response
    }

    suspend fun register(
        email: String,
        password: String,
        firstName: String? = null,
        lastName: String? = null,
    ): Result<AuthResponse> = runCatching {
        val response = api.register(RegisterRequest(email, password, firstName, lastName))
        tokenStorage.saveToken(response.token)
        response
    }

    suspend fun googleLogin(idToken: String, email: String, name: String?, picture: String?): Result<AuthResponse> = runCatching {
        val response = api.googleLogin(
            GoogleLoginRequest(
                token = idToken,
                userInfo = GoogleUserInfo(email = email, name = name, picture = picture),
            )
        )
        tokenStorage.saveToken(response.token)
        response
    }

    suspend fun getMe(): Result<UserDto> = runCatching { api.getMe() }

    fun isLoggedIn(): Boolean = tokenStorage.isLoggedIn()

    fun logout() = tokenStorage.clearToken()
}

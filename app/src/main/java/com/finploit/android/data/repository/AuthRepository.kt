package com.finploit.android.data.repository

import com.finploit.android.data.api.AuthApi
import com.finploit.android.data.dto.AuthResponse
import com.finploit.android.data.dto.GoogleLoginRequest
import com.finploit.android.data.dto.GoogleUserInfo
import com.finploit.android.data.dto.LoginRequest
import com.finploit.android.data.dto.RegisterRequest
import com.finploit.android.data.dto.UpdateProfileRequest
import com.finploit.android.data.dto.UserDto
import com.finploit.android.data.local.TokenStorage
import com.finploit.android.data.preferences.UserPreferencesRepository
import com.google.gson.Gson
import com.google.gson.JsonObject
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

private fun <T> Result<T>.mapHttpError(): Result<T> =
    recoverCatching { e ->
        if (e is HttpException) throw Exception(e.apiMessage())
        throw e
    }

private fun HttpException.apiMessage(): String {
    return try {
        val body = response()?.errorBody()?.string() ?: return message ?: "Erro desconhecido"
        val json = Gson().fromJson(body, JsonObject::class.java)
        when {
            json.has("message") && json.get("message").isJsonPrimitive ->
                json.get("message").asString
            json.has("message") && json.get("message").isJsonArray ->
                json.get("message").asJsonArray.joinToString(", ") { it.asString }
            else -> message ?: "Erro desconhecido"
        }
    } catch (e: Exception) {
        message ?: "Erro desconhecido"
    }
}

@Singleton
class AuthRepository @Inject constructor(
    private val api: AuthApi,
    private val tokenStorage: TokenStorage,
    private val preferencesRepository: UserPreferencesRepository,
) {
    suspend fun login(email: String, password: String): Result<AuthResponse> =
        runCatching {
            val response = api.login(LoginRequest(email, password))
            tokenStorage.saveToken(response.token)
            response.user.currency?.let { preferencesRepository.setCurrencyCode(it) }
            response
        }.mapHttpError()

    suspend fun register(
        email: String,
        password: String,
        firstName: String? = null,
        lastName: String? = null,
    ): Result<AuthResponse> =
        runCatching {
            val response = api.register(RegisterRequest(email, password, firstName, lastName))
            tokenStorage.saveToken(response.token)
            response.user.currency?.let { preferencesRepository.setCurrencyCode(it) }
            response
        }.mapHttpError()

    suspend fun updateCurrency(currencyCode: String): Result<Unit> =
        runCatching {
            api.updateProfile(UpdateProfileRequest(currency = currencyCode))
            preferencesRepository.setCurrencyCode(currencyCode)
        }.mapHttpError()

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

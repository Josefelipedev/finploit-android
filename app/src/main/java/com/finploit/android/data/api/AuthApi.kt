package com.finploit.android.data.api

import com.finploit.android.data.dto.AuthResponse
import com.finploit.android.data.dto.GoogleLoginRequest
import com.finploit.android.data.dto.LoginRequest
import com.finploit.android.data.dto.RegisterRequest
import com.finploit.android.data.dto.UpdateProfileRequest
import com.finploit.android.data.dto.UserDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST

interface AuthApi {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): AuthResponse

    @POST("auth/google")
    suspend fun googleLogin(@Body request: GoogleLoginRequest): AuthResponse

    @GET("auth/me")
    suspend fun getMe(): UserDto

    @PATCH("contacts/update")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): UserDto
}

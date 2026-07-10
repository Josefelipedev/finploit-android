package com.finploit.android.data.dto

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    val email: String,
    val password: String,
)

data class RegisterRequest(
    val email: String,
    val password: String,
    val firstName: String? = null,
    val lastName: String? = null,
    val phone: String? = null,
)

data class GoogleLoginRequest(
    val token: String,
    val userInfo: GoogleUserInfo,
)

data class GoogleUserInfo(
    val email: String,
    val name: String? = null,
    val picture: String? = null,
    val sub: String? = null,
)

data class AuthResponse(
    val token: String,
    val user: UserDto,
)

data class UserDto(
    val id: Int,
    val email: String,
    val name: String?,
    val displayName: String?,
    @SerializedName("profilePicUrl") val profilePicUrl: String?,
    val phone: String?,
    val firstName: String?,
    val lastName: String?,
    val currency: String? = null,
)

data class UpdateProfileRequest(
    val currency: String? = null,
    val name: String? = null,
    val displayName: String? = null,
    val fiscalCountry: String? = null,
    val fiscalRegime: String? = null,
    val activityStartDate: String? = null,
    val fiscalNumber: String? = null,
)

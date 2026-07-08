package com.finploit.android.data.api

import com.finploit.android.data.dto.CoupleInvitesResponse
import com.finploit.android.data.dto.CoupleProfileDto
import com.finploit.android.data.dto.CreateCoupleInviteRequest
import com.finploit.android.data.dto.CreateCoupleInviteResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface CoupleApi {
    @GET("contacts/profile")
    suspend fun getProfile(): CoupleProfileDto

    @GET("contacts/couple/invites")
    suspend fun listInvites(): CoupleInvitesResponse

    @POST("contacts/couple/invites")
    suspend fun createInvite(@Body request: CreateCoupleInviteRequest): CreateCoupleInviteResponse

    @POST("contacts/couple/invites/{id}/accept")
    suspend fun acceptInvite(@Path("id") id: Int): String

    @POST("contacts/couple/invites/{id}/reject")
    suspend fun rejectInvite(@Path("id") id: Int): String

    @DELETE("contacts/couple/invites/{id}")
    suspend fun cancelInvite(@Path("id") id: Int): String

    @DELETE("contacts/couple")
    suspend fun unlink(): String
}

package com.finploit.android.data.repository

import com.finploit.android.data.api.CoupleApi
import com.finploit.android.data.dto.CoupleInvitesResponse
import com.finploit.android.data.dto.CoupleProfileDto
import com.finploit.android.data.dto.CreateCoupleInviteRequest
import com.finploit.android.data.dto.CreateCoupleInviteResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CoupleRepository @Inject constructor(
    private val api: CoupleApi,
) {
    suspend fun getProfile(): Result<CoupleProfileDto> = runCatching { api.getProfile() }

    suspend fun listInvites(): Result<CoupleInvitesResponse> = runCatching { api.listInvites() }

    suspend fun createInvite(spousePhone: String): Result<CreateCoupleInviteResponse> =
        runCatching { api.createInvite(CreateCoupleInviteRequest(spousePhone)) }

    suspend fun acceptInvite(id: Int): Result<String> = runCatching { api.acceptInvite(id) }

    suspend fun rejectInvite(id: Int): Result<String> = runCatching { api.rejectInvite(id) }

    suspend fun cancelInvite(id: Int): Result<String> = runCatching { api.cancelInvite(id) }

    suspend fun unlink(): Result<String> = runCatching { api.unlink() }
}

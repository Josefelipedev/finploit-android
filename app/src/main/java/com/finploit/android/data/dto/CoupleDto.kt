package com.finploit.android.data.dto

data class CoupleProfileDto(
    val id: Int,
    val name: String?,
    val displayName: String?,
    val phone: String?,
    val isMarried: Boolean?,
    val spouseId: Int?,
    val spouse: SpouseDto?,
)

data class SpouseDto(
    val id: Int,
    val name: String?,
    val phone: String?,
)

data class CoupleInvitePartyDto(
    val id: Int,
    val name: String?,
    val displayName: String?,
    val phone: String?,
)

data class CoupleInviteDto(
    val id: Int,
    val status: String,
    val createdAt: String?,
    val expiresAt: String?,
    val inviter: CoupleInvitePartyDto,
    val invitee: CoupleInvitePartyDto,
)

data class CoupleInvitesResponse(
    val sent: List<CoupleInviteDto>,
    val received: List<CoupleInviteDto>,
)

data class CreateCoupleInviteRequest(
    val spousePhone: String,
)

data class CreateCoupleInviteResponse(
    val invite: CoupleInviteDto?,
    val message: String,
)

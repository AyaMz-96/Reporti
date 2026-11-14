package com.example.reporti.data

import com.google.gson.annotations.SerializedName

data class AdminUserDto(
    val id: String,
    val email: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("banned_until") val bannedUntil: String? = null,
    @SerializedName("user_metadata") val userMetadata: UserMetadataDto? = null,
    @SerializedName("app_metadata") val appMetadata: AppMetadataDto? = null,
)

data class UserMetadataDto(
    @SerializedName("full_name") val fullName: String? = null,
    val phone: String? = null,
    val categoryIds: List<Any?>? = null,
    val categoryId: Any? = null,
    val sectorId: Any? = null,
)

data class AppMetadataDto(val role: String? = null)

data class AdminUsersResponse(val users: List<AdminUserDto> = emptyList())

data class AdminUser(
    val id: String,
    val email: String,
    val fullName: String,
    val phone: String?,
    val role: String,
    val suspended: Boolean,
    val createdAt: String,
    val categoryIds: List<Long>,
    val sectorId: Long?,
) {
    companion object {
        private fun toLong(v: Any?): Long? = when (v) {
            null -> null
            is Number -> v.toLong()
            is String -> v.toLongOrNull()
            else -> null
        }

        fun from(dto: AdminUserDto): AdminUser {
            val rawRole = dto.appMetadata?.role
            val role = if (rawRole == "admin" || rawRole == "agent" || rawRole == "sector_manager")
                rawRole else "citizen"
            val meta = dto.userMetadata
            val cats: List<Long> = when {
                meta?.categoryIds != null -> meta.categoryIds.mapNotNull { toLong(it) }
                meta?.categoryId != null -> listOfNotNull(toLong(meta.categoryId))
                else -> emptyList()
            }
            return AdminUser(
                id = dto.id,
                email = dto.email ?: "",
                fullName = meta?.fullName ?: "",
                phone = meta?.phone,
                role = role,
                suspended = !dto.bannedUntil.isNullOrBlank(),
                createdAt = dto.createdAt ?: "",
                categoryIds = cats,
                sectorId = meta?.sectorId?.let { toLong(it) },
            )
        }
    }
}

data class CreateAdminUserBody(
    val email: String,
    val password: String,
    val fullName: String,
    val phone: String?,
    val role: String,
    val categoryIds: List<Long>?,
    val sectorId: Long?,
)

data class UpdateAdminUserBody(
    val fullName: String? = null,
    val phone: String? = null,
    val role: String? = null,
    val suspended: Boolean? = null,
    val categoryIds: List<Long>? = null,
    val sectorId: Long? = null,
)

data class CategoryBody(val name: String, val description: String?)

data class AssignReportBody(val agentId: String)

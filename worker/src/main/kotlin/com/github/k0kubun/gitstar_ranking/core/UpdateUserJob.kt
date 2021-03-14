package com.github.k0kubun.gitstar_ranking.core

// Either user_id or user_name is not null
data class UpdateUserJob(
    val id: Int,
    val userId: Long?,
    val userName: String?,
    val tokenUserId: Long,
)

package com.github.k0kubun.gitstar_ranking.core

class UpdateUserJob(
    val id: Int,
    // Either user_id or user_name is not null
    userId: Long?,
    val userName: String?,
    val tokenUserId: Long,
) {
    val userId: Long? = if (userId == 0L) null else userId
}

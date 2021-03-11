package com.github.k0kubun.gitstar_ranking.model

class UpdateUserJob(val id: Int, userId: Long, val userName: String) {
    val userId: Long? = if (userId == 0L) null else userId
}

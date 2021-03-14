package com.github.k0kubun.gitstar_ranking.core

import java.sql.Timestamp

data class User(
    val id: Long,
    val type: String,
) {
    val isOrganization: Boolean = (type == "Organization")
    var login: String? = null
    var stargazersCount = 0
    var avatarUrl: String? = null
    var updatedAt: Timestamp? = null
}

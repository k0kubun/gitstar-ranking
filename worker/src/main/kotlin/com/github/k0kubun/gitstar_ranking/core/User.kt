package com.github.k0kubun.gitstar_ranking.core

import java.sql.Timestamp

data class User(
    val id: Long,
    val type: String,
    val login: String? = null,
    val avatarUrl: String? = null,
) {
    val isOrganization: Boolean = (type == "Organization")
    var stargazersCount = 0L
    var updatedAt: Timestamp? = null
}

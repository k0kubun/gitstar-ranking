package com.github.k0kubun.gitstar_ranking.core

import java.sql.Timestamp

class User(val id: Long, val type: String) {
    var login: String? = null
    var stargazersCount = 0
    var avatarUrl: String? = null
    var updatedAt: Timestamp? = null
    val isOrganization: Boolean
        get() = type == "Organization"
}

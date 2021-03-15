package com.github.k0kubun.gitstar_ranking.core

import java.sql.Timestamp

data class User(
    val id: Long,
    val type: String,
    var stargazersCount: Long,
    var updatedAt: Timestamp?,
    val login: String? = null,
    val avatarUrl: String? = null,
)

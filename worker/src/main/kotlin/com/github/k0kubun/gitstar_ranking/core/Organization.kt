package com.github.k0kubun.gitstar_ranking.core

import java.sql.Timestamp

data class Organization(
    val id: Long,
    val login: String,
    val stargazersCount: Long,
    val updatedAt: Timestamp,
)

package com.github.k0kubun.gitstar_ranking.core

import java.sql.Timestamp

class Organization(
    val id: Int,
    val login: String,
    val stargazersCount: Int,
    val updatedAt: Timestamp,
)

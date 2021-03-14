package com.github.k0kubun.gitstar_ranking.core

data class Repository(
    val id: Long,
    val ownerId: Int? = null, // TODO: should be Long
    val name: String? = null, // TODO: can we remove this to save storage?
    val fullName: String? = null,
    val description: String? = null,
    val fork: Boolean? = null,
    val homepage: String? = null,
    val stargazersCount: Int,
    val language: String? = null,
)

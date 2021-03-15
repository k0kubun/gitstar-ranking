package com.github.k0kubun.gitstar_ranking.client

import com.github.k0kubun.gitstar_ranking.db.AccessTokenQuery
import org.jooq.DSLContext

class GitHubClientBuilder(private val database: DSLContext) {
    fun buildForUser(userId: Long): GitHubClient {
        val token = AccessTokenQuery(database).findToken(userId = userId)
        return GitHubClient(
            userId = userId,
            token = token!!,
        )
    }
}

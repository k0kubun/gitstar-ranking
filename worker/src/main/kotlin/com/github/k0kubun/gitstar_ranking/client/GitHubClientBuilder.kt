package com.github.k0kubun.gitstar_ranking.client

import com.github.k0kubun.gitstar_ranking.db.AccessTokenQuery
import org.jooq.DSLContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class GitHubClientBuilder(private val database: DSLContext) {
    fun buildForUser(
        userId: Long,
        logger: Logger = LoggerFactory.getLogger(GitHubClient::class.simpleName),
    ): GitHubClient {
        val token = AccessTokenQuery(database).findToken(userId = userId)
        return GitHubClient(userId = userId, token = token!!, logger = logger)
    }
}

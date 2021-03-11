package com.github.k0kubun.gitstar_ranking.github

import org.skife.jdbi.v2.DBI
import com.github.k0kubun.gitstar_ranking.repository.dao.AccessTokenDao
import javax.sql.DataSource

// This will have the logic to throttle GitHub API tokens.
class GitHubClientBuilder(dataSource: DataSource?) {
    private val dbi: DBI = DBI(dataSource)

    fun buildForUser(userId: Long?): GitHubClient {
        val token = dbi.onDemand(AccessTokenDao::class.java).findByUserId(userId)
        return GitHubClient(token!!.token)
    }
}

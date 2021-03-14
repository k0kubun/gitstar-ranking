package com.github.k0kubun.gitstar_ranking.client

import org.skife.jdbi.v2.DBI
import com.github.k0kubun.gitstar_ranking.db.AccessTokenDao
import javax.sql.DataSource

class GitHubClientBuilder(dataSource: DataSource?) {
    private val dbi: DBI = DBI(dataSource)

    fun buildForUser(userId: Long): GitHubClient {
        val token = dbi.onDemand(AccessTokenDao::class.java).findByUserId(userId)
        return GitHubClient(token!!)
    }
}

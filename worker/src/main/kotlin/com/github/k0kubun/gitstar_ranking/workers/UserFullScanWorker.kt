package com.github.k0kubun.gitstar_ranking.workers

import com.github.k0kubun.gitstar_ranking.GitstarRankingConfiguration
import com.github.k0kubun.gitstar_ranking.client.GitHubClient
import com.github.k0kubun.gitstar_ranking.client.GitHubClientBuilder
import com.github.k0kubun.gitstar_ranking.core.User
import com.github.k0kubun.gitstar_ranking.db.FULL_SCAN_USER_ID
import com.github.k0kubun.gitstar_ranking.db.LastUpdateQuery
import com.github.k0kubun.gitstar_ranking.db.UserDao
import com.github.k0kubun.gitstar_ranking.db.UserQuery
import java.sql.Timestamp
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.BlockingQueue
import java.util.concurrent.TimeUnit
import org.skife.jdbi.v2.DBI
import org.skife.jdbi.v2.Handle
import org.slf4j.LoggerFactory

private const val TOKEN_USER_ID: Long = 3138447 // k0kubun
private const val THRESHOLD_DAYS: Long = 1 // At least later than Mar 6th
private const val MIN_RATE_LIMIT_REMAINING: Long = 500 // Limit: 5000 / h

class UserFullScanWorker(config: GitstarRankingConfiguration) : UpdateUserWorker(config.database.dataSource, config.database.dslContext) {
    private val logger = LoggerFactory.getLogger(UserFullScanWorker::class.simpleName)
    private val userFullScanQueue: BlockingQueue<Boolean> = config.queue.userFullScanQueue
    private val updateThreshold: Timestamp = Timestamp.from(Instant.now().minus(THRESHOLD_DAYS, ChronoUnit.DAYS))
    override val dbi: DBI = DBI(config.database.dataSource)
    private val database = config.database.dslContext
    private val clientBuilder: GitHubClientBuilder = GitHubClientBuilder(config.database.dslContext)

    override fun perform() {
        while (userFullScanQueue.poll(5, TimeUnit.SECONDS) == null) {
            if (isStopped) {
                return
            }
        }
        val client = clientBuilder.buildForUser(TOKEN_USER_ID)
        logger.info("----- started UserFullScanWorker (API: ${client.rateLimitRemaining}/5000) -----")
        dbi.open().use { handle ->
            val lastUserId = handle.attach(UserDao::class.java).lastId()

            // 2 * (1000 / 30 min) ≒ 4000 / hour
            var i = 0
            while (i < 10) {
                var lastUpdatedId = LastUpdateQuery(database).findCursor(key = FULL_SCAN_USER_ID) ?: 0L
                val users = client.getUsersSince(lastUpdatedId)
                if (users.isEmpty()) {
                    break
                }
                handle.attach(UserDao::class.java).bulkInsert(users)
                for (user in users) {
                    if (PENDING_USERS.contains(user.login)) {
                        logger.info("Skipping a user with too many repositories: ${user.login}")
                        continue
                    }

                    val updatedAt = handle.attach(UserDao::class.java).userUpdatedAt(user.id)!! // TODO: Fix N+1
                    if (updatedAt.before(updateThreshold)) {
                        // Check rate limit
                        val remaining = client.rateLimitRemaining
                        logger.info("API remaining: $remaining/5000")
                        if (remaining < MIN_RATE_LIMIT_REMAINING) {
                            logger.info("API remaining is smaller than $MIN_RATE_LIMIT_REMAINING. Stopping.")
                            i = 10
                            break
                        }
                        updateUser(handle, UserQuery(database).find(id = user.id)!!, client) // TODO: Remove the user conversion, or at least fix N+1?
                        logger.info(String.format("[${user.login}] userId = ${user.id} / $lastUserId (%.4f%%)", 100.0 * user.id / lastUserId))
                    } else {
                        logger.info("Skip up-to-date user (id: ${user.id}, login: ${user.login}, updatedAt: $updatedAt)")
                    }
                    if (lastUpdatedId < user.id) {
                        lastUpdatedId = user.id
                    }
                    if (isStopped) { // Shutdown immediately if requested
                        break
                    }
                }
                LastUpdateQuery(database).update(key = FULL_SCAN_USER_ID, cursor = lastUpdatedId)
                i++
            }
        }
        logger.info("----- finished UserFullScanWorker (API: ${client.rateLimitRemaining}/5000) -----")
    }

    override fun updateUser(handle: Handle, user: User, client: GitHubClient) {
        super.updateUser(handle, user, client)
        Thread.sleep(500) // 0.5s: 1000 * 0.5s = 500s = 8.3 min (out of 15 min)
    }
}

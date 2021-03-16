package com.github.k0kubun.gitstar_ranking.workers

import com.github.k0kubun.gitstar_ranking.GitstarRankingConfiguration
import com.github.k0kubun.gitstar_ranking.client.GitHubClient
import com.github.k0kubun.gitstar_ranking.client.GitHubClientBuilder
import com.github.k0kubun.gitstar_ranking.db.FULL_SCAN_USER_ID
import com.github.k0kubun.gitstar_ranking.db.LastUpdateQuery
import com.github.k0kubun.gitstar_ranking.db.UserQuery
import java.sql.Timestamp
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.BlockingQueue
import java.util.concurrent.TimeUnit
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private const val TOKEN_USER_ID: Long = 3138447 // k0kubun
private const val THRESHOLD_DAYS: Long = 7 // At least later than Mar 6th
private const val MIN_RATE_LIMIT_REMAINING: Long = 500 // Limit: 5000 / h

class UserFullScanWorker(config: GitstarRankingConfiguration) : UserUpdateWorker(config.database.dslContext) {
    private val logger = LoggerFactory.getLogger(UserFullScanWorker::class.simpleName)
    private val userFullScanQueue: BlockingQueue<Boolean> = config.queue.userFullScanQueue
    private val updateThreshold: Timestamp = Timestamp.from(Instant.now().minus(THRESHOLD_DAYS, ChronoUnit.DAYS))
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
        val lastUserId = UserQuery(database).max("id") ?: 0L

        // 2 * (1000 / 30 min) ≒ 4000 / hour
        var i = 0
        while (i < 10) {
            var lastUpdatedId = LastUpdateQuery(database).findCursor(key = FULL_SCAN_USER_ID) ?: 0L
            val users = client.getUsersSince(lastUpdatedId)
            if (users.isEmpty()) {
                logger.info("No newer user was found")
                break
            }
            for (user in users) {
                if (PENDING_USERS.contains(user.login)) {
                    logger.info("Skipping a user with too many repositories: ${user.login}")
                    continue
                }

                val oldUser = UserQuery(database).find(id = user.id)
                if (oldUser == null || oldUser.updatedAt.before(updateThreshold)) {
                    // Check rate limit
                    logger.info("[${user.login}] userId = ${user.id} / $lastUserId (${String.format("%.4f%%", 100.0 * user.id / lastUserId)}), API remaining: ${client.rateLimitRemaining}/5000") // TODO: show this from updateUserId
                    if (client.rateLimitRemaining < MIN_RATE_LIMIT_REMAINING) {
                        logger.info("API remaining ${client.rateLimitRemaining} is smaller than $MIN_RATE_LIMIT_REMAINING. Stopping.")
                        i = 10
                        break
                    }
                    updateUserId(userId = user.id, client = client, logger = logger)
                } else {
                    logger.info("[${user.login}] Skip up-to-date user (id: ${user.id}, updatedAt: ${oldUser.updatedAt})")
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
        logger.info("----- finished UserFullScanWorker (API: ${client.rateLimitRemaining}/5000) -----")
    }

    override fun updateUserId(userId: Long, client: GitHubClient, logger: Logger) {
        super.updateUserId(userId = userId, client = client, logger = logger)
        Thread.sleep(200) // Doing this here to avoid sleeping when skipped
    }
}

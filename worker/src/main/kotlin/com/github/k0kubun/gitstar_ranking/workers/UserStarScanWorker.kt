package com.github.k0kubun.gitstar_ranking.workers

import com.github.k0kubun.gitstar_ranking.GitstarRankingConfiguration
import com.github.k0kubun.gitstar_ranking.client.GitHubClient
import com.github.k0kubun.gitstar_ranking.client.GitHubClientBuilder
import com.github.k0kubun.gitstar_ranking.core.User
import com.github.k0kubun.gitstar_ranking.db.LastUpdateQuery
import com.github.k0kubun.gitstar_ranking.db.STAR_SCAN_STARS
import com.github.k0kubun.gitstar_ranking.db.STAR_SCAN_USER_ID
import com.github.k0kubun.gitstar_ranking.db.UserQuery
import java.sql.Timestamp
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.BlockingQueue
import java.util.concurrent.TimeUnit
import org.jooq.impl.DSL.using
import org.slf4j.LoggerFactory

val PENDING_USERS = listOf(
    // Users with too many repositories. To be fixed later.
    "GITenberg",
    "gitpan",
    "the-domains",
    "wp-plugins",
    "gitter-badger",
    // Somehow 502?
    "Try-Git",
)

private const val TOKEN_USER_ID: Long = 3138447 // k0kubun
private const val THRESHOLD_DAYS: Long = 7 // At least later than Mar 6th
private const val MIN_RATE_LIMIT_REMAINING: Long = 500 // Limit: 5000 / h
private const val BATCH_SIZE = 100

// Scan all starred users
class UserStarScanWorker(config: GitstarRankingConfiguration) : UpdateUserWorker(config.database.dslContext) {
    private val logger = LoggerFactory.getLogger(UserStarScanWorker::class.simpleName)
    private val userStarScanQueue: BlockingQueue<Boolean> = config.queue.userStarScanQueue
    private val database = config.database.dslContext
    private val clientBuilder: GitHubClientBuilder = GitHubClientBuilder(config.database.dslContext)
    private val updateThreshold: Timestamp = Timestamp.from(Instant.now().minus(THRESHOLD_DAYS, ChronoUnit.DAYS))

    override fun perform() {
        while (userStarScanQueue.poll(5, TimeUnit.SECONDS) == null) {
            if (isStopped) {
                return
            }
        }
        val client = clientBuilder.buildForUser(TOKEN_USER_ID)
        logger.info("----- started UserStarScanWorker (API: ${client.rateLimitRemaining}/5000) -----")
        var numUsers = 1000 // 2 * (1000 / 30 min) ≒ 4000 / hour
        var numChecks = 2000 // Avoid issuing too many queries by skips
        while (numUsers > 0 && numChecks > 0 && !isStopped) {
            // Find a current cursor
            var lastUpdatedId = LastUpdateQuery(database).findCursor(key = STAR_SCAN_USER_ID) ?: 0L
            var stars = LastUpdateQuery(database).findCursor(key = STAR_SCAN_STARS) ?: 0L
            if (stars == 0L) {
                stars = UserQuery(database).max("stargazers_count") ?: 0L
            }

            // Query a next batch
            var users = emptyList<User>()
            while (users.isEmpty()) {
                users = UserQuery(database).orderByIdAsc(
                    stargazersCount = stars, idAfter = lastUpdatedId, limit = numUsers.coerceAtMost(BATCH_SIZE),
                )
                if (users.isEmpty()) {
                    stars = UserQuery(database).findStargazersCount(stargazersCountLessThan = stars) ?: 0L
                    if (stars == 0L) {
                        LastUpdateQuery(database).delete(key = listOf(STAR_SCAN_USER_ID, STAR_SCAN_STARS))
                        logger.info("--- completed and reset UserStarScanWorker (API: ${client.rateLimitRemaining}/5000) ---")
                        return
                    }
                    lastUpdatedId = 0
                }
            }

            // Update users in the batch
            logger.info("Batch size: ${users.size} (stars: $stars)")
            for (user in users) {
                if (PENDING_USERS.contains(user.login)) {
                    logger.info("Skipping a user with too many repositories: ${user.login}")
                    continue
                }

                // Check rate limit
                val remaining = client.rateLimitRemaining
                logger.info("API remaining: $remaining/5000 (numUsers: $numUsers, numChecks: $numChecks)")
                if (remaining < MIN_RATE_LIMIT_REMAINING) {
                    logger.info("API remaining is smaller than $remaining. Stopping.")
                    numChecks = 0
                    break
                }
                val updatedAt = UserQuery(database).find(id = user.id)!!.updatedAt // TODO: Fix N+1
                if (updatedAt.before(updateThreshold)) {
                    updateUser(user, client, TOKEN_USER_ID)
                    logger.info("[${user.login}] userId = ${user.id} (stars: ${user.stargazersCount})")
                    numUsers--
                } else {
                    logger.info("Skip up-to-date user (id: ${user.id}, login: ${user.login}, updatedAt: ${updatedAt})")
                }
                numChecks--
                if (lastUpdatedId < user.id) {
                    lastUpdatedId = user.id
                }
                if (isStopped) { // Shutdown immediately if requested
                    break
                }
            }

            // Update the counter
            val nextUpdatedId = lastUpdatedId
            val nextStars = stars
            database.transaction { tx ->
                LastUpdateQuery(using(tx)).update(key = STAR_SCAN_USER_ID, cursor = nextUpdatedId)
                LastUpdateQuery(using(tx)).update(key = STAR_SCAN_STARS, cursor = nextStars)
            }
        }
        logger.info("----- finished UserStarScanWorker (API: ${client.rateLimitRemaining}/5000) -----")
    }

    override fun updateUser(user: User, client: GitHubClient, tokenUserId: Long) {
        super.updateUser(user, client, tokenUserId)
        Thread.sleep(500) // 0.5s: 1000 * 0.5s = 500s = 8.3 min (out of 15 min)
    }
}

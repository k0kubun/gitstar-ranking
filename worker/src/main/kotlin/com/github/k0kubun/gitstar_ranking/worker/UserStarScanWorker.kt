package com.github.k0kubun.gitstar_ranking.worker

import com.github.k0kubun.gitstar_ranking.config.Config
import java.util.concurrent.BlockingQueue
import org.skife.jdbi.v2.DBI
import com.github.k0kubun.gitstar_ranking.github.GitHubClientBuilder
import kotlin.Throws
import java.lang.Exception
import java.util.concurrent.TimeUnit
import com.github.k0kubun.gitstar_ranking.github.GitHubClient
import com.github.k0kubun.gitstar_ranking.model.User
import com.github.k0kubun.gitstar_ranking.repository.dao.LastUpdateDao
import com.github.k0kubun.gitstar_ranking.repository.dao.UserDao
import org.skife.jdbi.v2.TransactionStatus
import java.io.IOException
import java.lang.InterruptedException
import java.sql.Timestamp
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.ArrayList
import org.skife.jdbi.v2.Handle
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

// Scan all starred users
class UserStarScanWorker(config: Config) : UpdateUserWorker(config.databaseConfig.dataSource) {
    private val userStarScanQueue: BlockingQueue<Boolean> = config.queueConfig.userStarScanQueue
    override val dbi: DBI = DBI(config.databaseConfig.dataSource)
    override val clientBuilder: GitHubClientBuilder = GitHubClientBuilder(config.databaseConfig.dataSource)
    private val updateThreshold: Timestamp = Timestamp.from(Instant.now().minus(THRESHOLD_DAYS, ChronoUnit.DAYS))

    @Throws(Exception::class)
    override fun perform() {
        while (userStarScanQueue.poll(5, TimeUnit.SECONDS) == null) {
            if (isStopped) {
                return
            }
        }
        val client = clientBuilder.buildForUser(TOKEN_USER_ID)
        LOG.info(String.format("----- started UserStarScanWorker (API: %s/5000) -----", client.rateLimitRemaining))
        dbi.open().use { handle ->
            var numUsers = 1000 // 2 * (1000 / 30 min) ≒ 4000 / hour
            var numChecks = 2000 // Avoid issuing too many queries by skips
            while (numUsers > 0 && numChecks > 0 && !isStopped) {
                // Find a current cursor
                var lastUpdatedId = handle.attach(LastUpdateDao::class.java).getCursor(LastUpdateDao.STAR_SCAN_USER_ID)
                var stars = handle.attach(LastUpdateDao::class.java).getCursor(LastUpdateDao.STAR_SCAN_STARS)
                if (stars == 0L) {
                    stars = handle.attach(UserDao::class.java).maxStargazersCount()
                }

                // Query a next batch
                var users: List<User> = ArrayList()
                while (users.isEmpty()) {
                    users = handle.attach(UserDao::class.java).usersWithStarsAfter(stars, lastUpdatedId, Math.min(numUsers, BATCH_SIZE))
                    if (users.isEmpty()) {
                        stars = handle.attach(UserDao::class.java).nextStargazersCount(stars)
                        if (stars == 0L) {
                            handle.useTransaction { conn: Handle, _: TransactionStatus? ->
                                conn.attach(LastUpdateDao::class.java).resetCursor(LastUpdateDao.STAR_SCAN_USER_ID)
                                conn.attach(LastUpdateDao::class.java).resetCursor(LastUpdateDao.STAR_SCAN_STARS)
                            }
                            LOG.info(String.format("--- completed and reset UserStarScanWorker (API: %s/5000) ---", client.rateLimitRemaining))
                            return
                        }
                        lastUpdatedId = 0
                    }
                }

                // Update users in the batch
                LOG.info(String.format("Batch size: %d (stars: %d)", users.size, stars))
                for (user in users) {
                    if (PENDING_USERS.contains(user.login)) {
                        LOG.info("Skipping a user with too many repositories: " + user.login)
                        continue
                    }

                    // Check rate limit
                    val remaining = client.rateLimitRemaining
                    LOG.info(String.format("API remaining: %d/5000 (numUsers: %d, numChecks: %d)", remaining, numUsers, numChecks))
                    if (remaining < MIN_RATE_LIMIT_REMAINING) {
                        LOG.info(String.format("API remaining is smaller than %d. Stopping.", remaining))
                        numChecks = 0
                        break
                    }
                    val updatedAt = handle.attach(UserDao::class.java).userUpdatedAt(user.id)!! // TODO: Fix N+1
                    if (updatedAt.before(updateThreshold)) {
                        updateUser(handle, user, client)
                        LOG.info(String.format("[%s] userId = %d (stars: %d)", user.login, user.id, user.stargazersCount))
                        numUsers--
                    } else {
                        LOG.info(String.format("Skip up-to-date user (id: %d, login: %s, updatedAt: %s)", user.id, user.login, updatedAt.toString()))
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
                handle.useTransaction { conn: Handle, _: TransactionStatus? ->
                    conn.attach(LastUpdateDao::class.java).updateCursor(LastUpdateDao.STAR_SCAN_USER_ID, nextUpdatedId)
                    conn.attach(LastUpdateDao::class.java).updateCursor(LastUpdateDao.STAR_SCAN_STARS, nextStars)
                }
            }
        }
        LOG.info(String.format("----- finished UserStarScanWorker (API: %s/5000) -----", client.rateLimitRemaining))
    }

    @Throws(IOException::class)
    override fun updateUser(handle: Handle, user: User, client: GitHubClient) {
        super.updateUser(handle, user, client)
        try {
            Thread.sleep(500) // 0.5s: 1000 * 0.5s = 500s = 8.3 min (out of 15 min)
        } catch (e: InterruptedException) {
            // suppress for override
        }
    }

    companion object {
        private const val TOKEN_USER_ID: Long = 3138447 // k0kubun
        private const val THRESHOLD_DAYS: Long = 1 // At least later than Mar 6th
        private const val MIN_RATE_LIMIT_REMAINING: Long = 500 // Limit: 5000 / h
        private const val BATCH_SIZE = 100
        private val LOG = LoggerFactory.getLogger(UserStarScanWorker::class.java)
    }

}

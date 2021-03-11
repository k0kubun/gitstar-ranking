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
import com.github.k0kubun.gitstar_ranking.repository.dao.UserDao
import com.github.k0kubun.gitstar_ranking.repository.dao.LastUpdateDao
import java.io.IOException
import java.lang.InterruptedException
import java.sql.Timestamp
import java.time.Instant
import java.time.temporal.ChronoUnit
import org.skife.jdbi.v2.Handle
import org.slf4j.LoggerFactory

class UserFullScanWorker(config: Config) : UpdateUserWorker(config.databaseConfig.dataSource) {
    private val userFullScanQueue: BlockingQueue<Boolean> = config.queueConfig.userFullScanQueue
    override val dbi: DBI = DBI(config.databaseConfig.dataSource)
    override val clientBuilder: GitHubClientBuilder = GitHubClientBuilder(config.databaseConfig.dataSource)
    private val updateThreshold: Timestamp = Timestamp.from(Instant.now().minus(THRESHOLD_DAYS, ChronoUnit.DAYS))

    @Throws(Exception::class)
    override fun perform() {
        while (userFullScanQueue.poll(5, TimeUnit.SECONDS) == null) {
            if (isStopped) {
                return
            }
        }
        val client = clientBuilder.buildForUser(TOKEN_USER_ID)
        LOG.info(String.format("----- started UserFullScanWorker (API: %s/5000) -----", client.rateLimitRemaining))
        dbi.open().use { handle ->
            val lastUserId = handle.attach(UserDao::class.java).lastId()

            // 2 * (1000 / 30 min) ≒ 4000 / hour
            var i = 0
            while (i < 10) {
                var lastUpdatedId = handle.attach(LastUpdateDao::class.java).getCursor(LastUpdateDao.FULL_SCAN_USER_ID)
                val users = client.getUsersSince(lastUpdatedId)
                if (users.isEmpty()) {
                    break
                }
                handle.attach(UserDao::class.java).bulkInsert(users)
                for (user in users) {
                    val updatedAt = handle.attach(UserDao::class.java).userUpdatedAt(user.id)!! // TODO: Fix N+1
                    if (updatedAt.before(updateThreshold)) {
                        // Check rate limit
                        val remaining = client.rateLimitRemaining
                        LOG.info(String.format("API remaining: %d/5000", remaining))
                        if (remaining < MIN_RATE_LIMIT_REMAINING) {
                            LOG.info(String.format("API remaining is smaller than %d. Stopping.", MIN_RATE_LIMIT_REMAINING))
                            i = 10
                            break
                        }
                        updateUser(handle, user, client)
                        LOG.info(String.format("[%s] userId = %d / %d (%.4f%%)",
                            user.login, user.id, lastUserId, 100.0 * user.id / lastUserId))
                    } else {
                        LOG.info(String.format("Skip up-to-date user (id: %d, login: %s, updatedAt: %s)", user.id, user.login, updatedAt.toString()))
                    }
                    if (lastUpdatedId < user.id) {
                        lastUpdatedId = user.id
                    }
                    if (isStopped) { // Shutdown immediately if requested
                        break
                    }
                }
                handle.attach(LastUpdateDao::class.java).updateCursor(LastUpdateDao.FULL_SCAN_USER_ID, lastUpdatedId)
                i++
            }
        }
        LOG.info(String.format("----- finished UserFullScanWorker (API: %s/5000) -----", client.rateLimitRemaining))
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
        private val LOG = LoggerFactory.getLogger(UserFullScanWorker::class.java)
    }
}

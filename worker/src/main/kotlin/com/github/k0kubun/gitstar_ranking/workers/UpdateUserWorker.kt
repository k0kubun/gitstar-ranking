package com.github.k0kubun.gitstar_ranking.workers

import org.skife.jdbi.v2.DBI
import com.github.k0kubun.gitstar_ranking.client.GitHubClientBuilder
import java.lang.Exception
import com.github.k0kubun.gitstar_ranking.db.DatabaseLock
import java.util.concurrent.TimeUnit
import java.lang.RuntimeException
import com.github.k0kubun.gitstar_ranking.client.GitHubClient
import com.github.k0kubun.gitstar_ranking.db.UserDao
import io.sentry.Sentry
import org.skife.jdbi.v2.TransactionStatus
import com.github.k0kubun.gitstar_ranking.db.RepositoryDao
import com.github.k0kubun.gitstar_ranking.core.Repository
import com.github.k0kubun.gitstar_ranking.core.UpdateUserJob
import com.github.k0kubun.gitstar_ranking.core.User
import com.github.k0kubun.gitstar_ranking.db.UpdateUserJobQuery
import java.sql.Timestamp
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.ArrayList
import javax.sql.DataSource
import javax.ws.rs.NotFoundException
import org.jooq.Configuration
import org.jooq.DSLContext
import org.jooq.impl.DSL.using
import org.skife.jdbi.v2.Handle
import org.slf4j.LoggerFactory

private const val TIMEOUT_MINUTES = 1

open class UpdateUserWorker(dataSource: DataSource?, private val database: DSLContext) : Worker() {
    private val logger = LoggerFactory.getLogger(UpdateUserWorker::class.simpleName)
    open val dbi: DBI = DBI(dataSource)
    private val lock = DatabaseLock(database)
    private val clientBuilder: GitHubClientBuilder = GitHubClientBuilder(database)

    // Dequeue a record from update_user_jobs and call updateUser().
    override fun perform() {
        // Poll until it succeeds to acquire a job...
        var job: UpdateUserJob? = null
        while (job == null) {
            if (isStopped) {
                return
            }
            val timeoutAt = nextTimeout()
            job = database.transactionResult { tx ->
                if (acquireUntil(tx, timeoutAt) != 0L) {
                    // Succeeded to acquire a job. Fetch job to execute.
                    UpdateUserJobQuery(using(tx)).find(timeoutAt = timeoutAt).also {
                        it ?: throw RuntimeException("Failed to fetch a job (timeoutAt = $timeoutAt)")
                    }
                } else null
            }
            if (job == null) {
                TimeUnit.SECONDS.sleep(1)
            }
        }

        try {
            dbi.open().use { handle ->
                val client = clientBuilder.buildForUser(job.tokenUserId)
                val userId: Long = job.userName?.let { userName ->
                    createUser(handle, userName, client).id
                } ?: job.userId!!
                lock.withUserUpdate(userId) {
                    val user = handle.attach(UserDao::class.java).find(userId)!!
                    logger.info("UpdateUserWorker started: (userId = $userId, login = ${user.login})")
                    updateUser(handle, user, client)
                    logger.info("UpdateUserWorker finished: (userId = $userId, login = ${user.login})") // TODO: Log elapsed time
                }
            }
        } catch (e: Exception) {
            Sentry.captureException(e)
            logger.error("Error in UpdateUserWorker! (userId = ${job.userId}: ${e.stackTraceToString()}")
        } finally {
            UpdateUserJobQuery(database).delete(id = job.id)
        }
    }

    // Create a pre-required user record for a give userName.
    private fun createUser(handle: Handle, userName: String, client: GitHubClient): User {
        val user = client.getUserWithLogin(userName)
        val users: MutableList<User> = ArrayList()
        users.add(user)
        handle.attach(UserDao::class.java).bulkInsert(users)
        return user
    }

    // Main part of this class. Given enqueued userId, it updates a user and his repositories.
    // * Sync information of all repositories owned by specified user.
    // * Update fetched_at and updated_at, and set total stars to user.
    // TODO: Requeue if GitHub API limit exceeded
    open fun updateUser(handle: Handle, user: User, client: GitHubClient) {
        val userId = user.id
        try {
            val newLogin = client.getLogin(userId) // TODO: Can we lazily this call using repository full_names?
            if (user.login != newLogin) {
                handle.attach(UserDao::class.java).updateLogin(userId, newLogin)
            }
        } catch (e: NotFoundException) {
            logger.error("User NotFoundException: ${e.message}")
            logger.info("Deleting user id: $userId, login: ${user.login}")
            handle.useTransaction { conn: Handle, _: TransactionStatus? ->
                conn.attach(UserDao::class.java).delete(userId)
                conn.attach(RepositoryDao::class.java).deleteAllOwnedBy(userId)
            }
            return
        }

        val repos = client.getPublicRepos(userId)
        val repoIds: MutableList<Long> = ArrayList()
        for (repo in repos) {
            repoIds.add(repo.id)
        }
        handle.useTransaction { conn: Handle, _: TransactionStatus? ->
            if (repoIds.size > 0) {
                conn.attach(RepositoryDao::class.java).deleteAllOwnedByExcept(userId, repoIds) // Delete obsolete ones
            } else {
                conn.attach(RepositoryDao::class.java).deleteAllOwnedBy(userId)
            }
            conn.attach(RepositoryDao::class.java).bulkInsert(repos)
            conn.attach(UserDao::class.java).updateStars(userId, calcTotalStars(repos))
        }
        logger.info("[${user.login}] imported repos: ${repos.size}")
    }

    // Concurrently executing `dao.acquireUntil` causes deadlock. So this executes it in a lock.
    private fun acquireUntil(tx: Configuration, timeoutAt: Timestamp): Long {
        return lock.withUpdateUserJobs { UpdateUserJobQuery(using(tx)).acquireUntil(timeoutAt) }
    }

    private fun nextTimeout(): Timestamp {
        return Timestamp.valueOf(LocalDateTime.now(ZoneId.of("UTC")).plusMinutes(TIMEOUT_MINUTES.toLong()))
    }

    private fun calcTotalStars(repos: List<Repository>): Long {
        var totalStars = 0L
        repos.forEach { repo ->
            totalStars += repo.stargazersCount
        }
        return totalStars
    }
}

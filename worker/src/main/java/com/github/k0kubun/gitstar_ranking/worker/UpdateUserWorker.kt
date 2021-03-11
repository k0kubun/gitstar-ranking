package com.github.k0kubun.gitstar_ranking.worker

import org.skife.jdbi.v2.DBI
import com.github.k0kubun.gitstar_ranking.github.GitHubClientBuilder
import kotlin.Throws
import java.lang.Exception
import com.github.k0kubun.gitstar_ranking.repository.DatabaseLock
import java.util.concurrent.TimeUnit
import com.github.k0kubun.gitstar_ranking.repository.dao.UpdateUserJobDao
import java.lang.RuntimeException
import com.github.k0kubun.gitstar_ranking.github.GitHubClient
import com.github.k0kubun.gitstar_ranking.repository.dao.UserDao
import io.sentry.Sentry
import java.io.IOException
import org.skife.jdbi.v2.TransactionStatus
import com.github.k0kubun.gitstar_ranking.repository.dao.RepositoryDao
import com.github.k0kubun.gitstar_ranking.github.GitHubClient.UserNotFoundException
import com.github.k0kubun.gitstar_ranking.model.Repository
import com.github.k0kubun.gitstar_ranking.model.User
import java.sql.Timestamp
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.ArrayList
import javax.sql.DataSource
import org.skife.jdbi.v2.Handle
import org.slf4j.LoggerFactory

open class UpdateUserWorker(dataSource: DataSource?) : Worker() {
    open val dbi: DBI
    open val clientBuilder: GitHubClientBuilder

    // Dequeue a record from update_user_jobs and call updateUser().
    @Throws(Exception::class)
    override fun perform() {
        dbi.open().use { handle ->
            dbi.open().use { lockHandle ->
                val lock = DatabaseLock(lockHandle)

                // Poll until it succeeds to acquire a job...
                var timeoutAt: Timestamp
                while (acquireUntil(lock, handle, nextTimeout().also { timeoutAt = it }) == 0L) {
                    if (isStopped) {
                        return
                    }
                    TimeUnit.SECONDS.sleep(1)
                }

                // Succeeded to acquire a job. Fetch job to execute.
                val dao = handle.attach(UpdateUserJobDao::class.java)
                val job = dao.fetchByTimeout(timeoutAt)
                    ?: throw RuntimeException("Failed to fetch a job (timeoutAt = $timeoutAt)")

                // TODO: Log elapsed time
                try {
                    val client = clientBuilder.buildForUser(job.tokenUserId)
                    val userId: Long
                    userId = if (job.userName == null) {
                        job.userId
                    } else {
                        createUser(handle, job.userName, client)
                    }
                    lock.withUserUpdate(userId) {
                        val user = handle.attach(UserDao::class.java).find(userId)
                        LOG.info("UpdateUserWorker started: (userId = " + userId + ", login = " + user.login + ")")
                        updateUser(handle, user, client)
                        LOG.info("UpdateUserWorker finished: (userId = " + userId + ", login = " + user.login + ")")
                    }
                } catch (e: Exception) {
                    Sentry.capture(e)
                    LOG.error("Error in UpdateUserWorker! (userId = " + job.userId + "): " + e.toString() + ": " + e.message)
                    // e.printStackTrace();
                } finally {
                    dao.delete(job.id)
                }
            }
        }
    }

    // Create a pre-required user record for a give userName.
    @Throws(IOException::class)
    private fun createUser(handle: Handle, userName: String, client: GitHubClient): Long {
        val user = client.getUserWithLogin(userName)
        val users: MutableList<User> = ArrayList()
        users.add(user)
        handle.attach(UserDao::class.java).bulkInsert(users)
        return user.id
    }

    // Main part of this class. Given enqueued userId, it updates a user and his repositories.
    // * Sync information of all repositories owned by specified user.
    // * Update fetched_at and updated_at, and set total stars to user.
    // TODO: Requeue if GitHub API limit exceeded
    @Throws(IOException::class)
    open fun updateUser(handle: Handle, user: User, client: GitHubClient) {
        val userId = user.id
        val login = user.login
        try {
            LOG.debug("[$login] finished: find User")
            if (!user.isOrganization) {
                val newLogin = client.getLogin(userId)
                if (newLogin != null) {
                    handle.attach(UserDao::class.java).updateLogin(userId, newLogin)
                }
                LOG.debug("[$login] finished: update Login")
            }
            val repos = client.getPublicRepos(userId, user.isOrganization)
            LOG.debug("[$login] finished: getPublicRepos")
            val repoIds: MutableList<Long> = ArrayList()
            for (repo in repos) {
                repoIds.add(repo.id)
            }
            handle.useTransaction { conn: Handle, status: TransactionStatus? ->
                if (repoIds.size > 0) {
                    conn.attach(RepositoryDao::class.java).deleteAllOwnedByExcept(userId, repoIds) // Delete obsolete ones
                } else {
                    conn.attach(RepositoryDao::class.java).deleteAllOwnedBy(userId)
                }
                conn.attach(RepositoryDao::class.java).bulkInsert(repos)
                LOG.debug("[$login] finished: bulkInsert")
                conn.attach(UserDao::class.java).updateStars(userId, calcTotalStars(repos))
                LOG.debug("[$login] finished: updateStars")
            }
            LOG.info("[" + login + "] imported repos: " + repos.size)
        } catch (e: UserNotFoundException) {
            LOG.error("UserNotFoundException error: " + e.message)
            LOG.info("delete user: $userId")
            handle.useTransaction { conn: Handle, status: TransactionStatus? ->
                conn.attach(UserDao::class.java).delete(userId)
                conn.attach(RepositoryDao::class.java).deleteAllOwnedBy(userId)
            }
        }
    }

    // Concurrently executing `dao.acquireUntil` causes deadlock. So this executes it in a lock.
    private fun acquireUntil(lock: DatabaseLock, handle: Handle, timeoutAt: Timestamp): Long {
        return lock.withUpdateUserJobs { handle.attach(UpdateUserJobDao::class.java).acquireUntil(timeoutAt) }
    }

    private fun nextTimeout(): Timestamp {
        return Timestamp.valueOf(LocalDateTime.now(ZoneId.of("UTC")).plusMinutes(TIMEOUT_MINUTES.toLong()))
    }

    private fun calcTotalStars(repos: List<Repository>): Int {
        var totalStars = 0
        for (repo in repos) {
            totalStars += repo.stargazersCount
        }
        return totalStars
    }

    companion object {
        private const val TIMEOUT_MINUTES = 1
        private val LOG = LoggerFactory.getLogger(UpdateUserWorker::class.java)
    }

    init {
        clientBuilder = GitHubClientBuilder(dataSource)
        dbi = DBI(dataSource)
    }
}

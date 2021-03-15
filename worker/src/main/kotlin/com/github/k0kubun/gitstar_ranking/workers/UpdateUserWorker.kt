package com.github.k0kubun.gitstar_ranking.workers

import com.github.k0kubun.gitstar_ranking.client.GitHubClient
import com.github.k0kubun.gitstar_ranking.client.GitHubClientBuilder
import com.github.k0kubun.gitstar_ranking.client.UserResponse
import com.github.k0kubun.gitstar_ranking.core.Repository
import com.github.k0kubun.gitstar_ranking.core.UpdateUserJob
import com.github.k0kubun.gitstar_ranking.core.User
import com.github.k0kubun.gitstar_ranking.db.DatabaseLock
import com.github.k0kubun.gitstar_ranking.db.RepositoryQuery
import com.github.k0kubun.gitstar_ranking.db.UpdateUserJobQuery
import com.github.k0kubun.gitstar_ranking.db.UserQuery
import io.sentry.Sentry
import java.lang.Exception
import java.lang.RuntimeException
import java.sql.Connection
import java.sql.Timestamp
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.ArrayList
import java.util.concurrent.TimeUnit
import javax.ws.rs.NotFoundException
import org.jooq.DSLContext
import org.jooq.impl.DSL.using
import org.slf4j.LoggerFactory

private const val TIMEOUT_MINUTES = 1

open class UpdateUserWorker(private val database: DSLContext) : Worker() {
    private val logger = LoggerFactory.getLogger(UpdateUserWorker::class.simpleName)
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
            job = database.connectionResult { conn ->
                if (acquireUntil(conn, timeoutAt) != 0L) {
                    // Succeeded to acquire a job. Fetch job to execute.
                    UpdateUserJobQuery(using(conn)).find(timeoutAt = timeoutAt).also {
                        it ?: throw RuntimeException("Failed to fetch a job (timeoutAt = $timeoutAt)")
                    }
                } else null
            }
            if (job == null) {
                TimeUnit.SECONDS.sleep(1)
            }
        }

        try {
            val client = clientBuilder.buildForUser(job.tokenUserId)
            val userId: Long = job.userName?.let { login ->
                createUser(login, client).id
            } ?: job.userId!!
            DatabaseLock(database).withUserUpdate(userId) {
                val user = UserQuery(database).find(id = userId)!!
                logger.info("UpdateUserWorker started: (userId = $userId, login = ${user.login})")
                updateUser(user, client)
                logger.info("UpdateUserWorker finished: (userId = $userId, login = ${user.login})") // TODO: Log elapsed time
            }
        } catch (e: Exception) {
            Sentry.captureException(e)
            logger.error("Error in UpdateUserWorker! (userId = ${job.userId}: ${e.stackTraceToString()}")
        } finally {
            UpdateUserJobQuery(database).delete(id = job.id)
        }
    }

    // Create a pre-required user record for a give userName.
    private fun createUser(login: String, client: GitHubClient): UserResponse {
        val user = client.getUserWithLogin(login)
        UserQuery(database).create(user)
        return user
    }

    // Main part of this class. Given enqueued userId, it updates a user and his repositories.
    // * Sync information of all repositories owned by specified user.
    // * Update fetched_at and updated_at, and set total stars to user.
    // TODO: Requeue if GitHub API limit exceeded
    open fun updateUser(user: User, client: GitHubClient) {
        val userId = user.id
        try {
            val newLogin = client.getLogin(userId) // TODO: Can we lazily this call using repository full_names?
            if (user.login != newLogin) {
                UserQuery(database).update(id = userId, login = newLogin)
            }
        } catch (e: NotFoundException) {
            logger.error("User NotFoundException: ${e.message}")
            logger.info("Deleting user id: $userId, login: ${user.login}")
            database.transaction { tx ->
                UserQuery(using(tx)).destroy(id = userId)
                RepositoryQuery(using(tx)).deleteAll(ownerId = userId)
            }
            return
        }

        val repos = client.getPublicRepos(userId)
        val repoIds: MutableList<Long> = ArrayList()
        for (repo in repos) {
            repoIds.add(repo.id)
        }
        database.transaction { tx ->
            RepositoryQuery(using(tx)).deleteAll(ownerId = userId)
            RepositoryQuery(using(tx)).insertAll(repos)
            UserQuery(using(tx)).update(id = userId, stargazersCount = calcTotalStars(repos))
        }
        logger.info("[${user.login}] imported repos: ${repos.size}")
    }

    // Concurrently executing `dao.acquireUntil` causes deadlock. So this executes it in a lock.
    private fun acquireUntil(conn: Connection, timeoutAt: Timestamp): Long {
        return DatabaseLock(database).withUpdateUserJobs {
            UpdateUserJobQuery(using(conn)).acquireUntil(timeoutAt)
        }
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

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
import org.slf4j.Logger
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
            val userId: Long = job.userName?.let { login -> // TODO: Unify to use user_id
                createUserByLogin(login, client).id
            } ?: job.userId!!
            updateUserId(userId = userId, client = client, logger = logger)
        } catch (e: Exception) {
            Sentry.captureException(e)
            logger.error("Error in UpdateUserWorker! (userId = ${job.userId}: ${e.stackTraceToString()}")
        } finally {
            UpdateUserJobQuery(database).delete(id = job.id)
        }
    }

    open fun updateUserId(userId: Long, client: GitHubClient, logger: Logger) {
        DatabaseLock(database).withUserUpdate(userId) {
            val user = UserQuery(database).find(id = userId) ?: createUserById(id = userId, client = client)
            if (user != null) {
                logger.info("updateUserId started (userId = $userId, login = ${user.login})")
                updateUser(user = user, client = client, logger = logger)
                logger.info("updateUserId finished: (userId = $userId, login = ${user.login})") // TODO: Log elapsed time
            }
        }
    }

    // Create a pre-required user record for a give userName.
    private fun createUserByLogin(login: String, client: GitHubClient): UserResponse {
        val user = client.getUserWithLogin(login)
        UserQuery(database).create(user)
        return user
    }

    private fun createUserById(id: Long, client: GitHubClient): User? {
        val user = try {
            client.getUser(userId = id)
        } catch (e: NotFoundException) {
            logger.debug("Skipping to create a user because user_id=$id didn't exist")
            return null
        }
        UserQuery(database).create(user)
        return UserQuery(database).find(id = id)!!
    }

    // Main part of this class. Given enqueued userId, it updates a user and his repositories.
    // * Sync information of all repositories owned by specified user.
    // * Update fetched_at and updated_at, and set total stars to user.
    // TODO: Requeue if GitHub API limit exceeded
    private fun updateUser(user: User, client: GitHubClient, logger: Logger) {
        val userId = user.id
        try {
            val newLogin = client.getLogin(userId) // TODO: Can we lazily this call using repository full_names?
            if (user.login != newLogin) {
                updateUserLogin(userId = userId, newLogin = newLogin, tokenUserId = client.userId)
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

    private fun updateUserLogin(userId: Long, newLogin: String, tokenUserId: Long) {
        database.transaction { tx ->
            val conflictUser = UserQuery(using(tx)).findBy(login = newLogin)
            if (conflictUser != null) { // Lazily recreate it to avoid recursive conflict handling here
                UserQuery(using(tx)).destroy(id = conflictUser.id)
                UpdateUserJobQuery(using(tx)).create(userId = conflictUser.id, tokenUserId = tokenUserId)
            }
            UserQuery(using(tx)).update(id = userId, login = newLogin)
        }
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

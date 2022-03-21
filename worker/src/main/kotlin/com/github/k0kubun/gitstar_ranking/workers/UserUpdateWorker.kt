package com.github.k0kubun.gitstar_ranking.workers

import com.github.k0kubun.gitstar_ranking.client.GitHubClient
import com.github.k0kubun.gitstar_ranking.client.GitHubClientBuilder
import com.github.k0kubun.gitstar_ranking.client.UserResponse
import com.github.k0kubun.gitstar_ranking.core.Repository
import com.github.k0kubun.gitstar_ranking.core.UserUpdateJob
import com.github.k0kubun.gitstar_ranking.core.User
import com.github.k0kubun.gitstar_ranking.db.DatabaseLock
import com.github.k0kubun.gitstar_ranking.db.RepositoryQuery
import com.github.k0kubun.gitstar_ranking.db.UserUpdateJobQuery
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
import org.jooq.Configuration
import org.jooq.DSLContext
import org.jooq.impl.DSL.using
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.ws.rs.ForbiddenException
import javax.ws.rs.NotAuthorizedException

private const val TIMEOUT_MINUTES = 1

open class UserUpdateWorker(
    private val database: DSLContext,
    private val logger: Logger = LoggerFactory.getLogger(UserUpdateWorker::class.simpleName),
) : Worker() {
    // Dequeue a record from update_user_jobs and call updateUser().
    override fun perform() {
        // Poll until it succeeds to acquire a job...
        var job: UserUpdateJob? = null
        while (job == null) {
            if (isStopped) {
                return
            }
            val timeoutAt = nextTimeout()
            job = database.connectionResult { conn ->
                if (acquireUntil(conn, timeoutAt) != 0L) {
                    // Succeeded to acquire a job. Fetch job to execute.
                    UserUpdateJobQuery(using(conn)).find(timeoutAt = timeoutAt).also {
                        it ?: throw RuntimeException("Failed to fetch a job (timeoutAt = $timeoutAt)")
                    }
                } else null
            }
            if (job == null) {
                TimeUnit.SECONDS.sleep(1)
            }
        }

        val client = try {
            GitHubClientBuilder(database).buildForUser(job.tokenUserId, logger = logger)
        } catch (e: NotAuthorizedException) {
            logger.error("Skipped Job(id: ${job.userId}, name: ${job.userName}, token: ${job.tokenUserId}) since the token is invalid")
            return
        }
        try {
            val userId: Long = job.userName?.let { login -> // TODO: Unify to use user_id
                createUserByLogin(login, client).id
            } ?: job.userId!!
            updateUserId(userId = userId, client = client)
        } catch (e: Exception) {
            Sentry.captureException(e)
            logger.error("Error in UpdateUserWorker! (userId = ${job.userId}: ${e.stackTraceToString()}")
        } finally {
            UserUpdateJobQuery(database).delete(id = job.id)
        }
    }

    open fun updateUserId(userId: Long, client: GitHubClient, sleepMillis: Long = 0) {
        DatabaseLock(database).withUserUpdate(userId) {
            val user = UserQuery(database).find(id = userId) ?: createUserById(id = userId, client = client)
            if (user != null) {
                logger.info("[${user.login}] updateUserId started  (userId = $userId)")
                val numRepos = updateUser(user = user, client = client)
                logger.info("[${user.login}] updateUserId finished (userId = $userId, imported $numRepos repos)")
            }
        }
        Thread.sleep(sleepMillis)
    }

    // Main part of this class. Given enqueued userId, it updates a user and his repositories.
    // * Sync information of all repositories owned by specified user.
    // * Update fetched_at and updated_at, and set total stars to user.
    // TODO: Requeue if GitHub API limit exceeded
    private fun updateUser(user: User, client: GitHubClient): Int {
        val userId = user.id
        try {
            val newLogin = client.getLogin(userId) // TODO: Can we lazily this call using repository full_names?
            if (user.login != newLogin) {
                updateUserLogin(userId = userId, newLogin = newLogin, tokenUserId = client.userId)
            }
        } catch (e: ForbiddenException) {
            logger.error("[${user.login}] ForbiddenException on getLogin, skipping: ${e.message}")
            return 0
        } catch (e: NotFoundException) {
            logger.error("[${user.login}] User NotFoundException on updateUser: ${e.message}")
            logger.info("[${user.login}] Deleting user id: $userId")
            database.transaction { tx ->
                UserQuery(using(tx)).destroy(id = userId)
                RepositoryQuery(using(tx)).deleteAll(ownerId = userId)
            }
            return 0
        }

        val repos = try {
            client.getPublicRepos(userId, logPrefix = "[${user.login}]")
        } catch (e: ForbiddenException) {
            logger.error("[${user.login}] ForbiddenException on getPublicRepos, skipping: ${e.message}")
            return 0
        }
        database.transaction { tx ->
            RepositoryQuery(using(tx)).deleteAll(fullNames = repos.map { it.fullName }) // just to avoid conflicts
            RepositoryQuery(using(tx)).deleteAll(ownerId = userId) // delete obsoleted repos
            RepositoryQuery(using(tx)).insertAll(repos)
            UserQuery(using(tx)).update(id = userId, stargazersCount = calcTotalStars(repos))
        }
        return repos.size
    }

    private fun createUserById(id: Long, client: GitHubClient): User? {
        val user = try {
            client.getUser(userId = id)
        } catch (e: NotFoundException) {
            logger.debug("Skipping to create a user because user_id=$id didn't exist")
            return null
        }
        database.transaction { tx ->
            tx.rebuildConflictUser(login = user.login, tokenUserId = client.userId)
            UserQuery(using(tx)).create(user)
        }
        return UserQuery(database).find(id = id)!!
    }

    // Create a pre-required user record for a give userName.
    private fun createUserByLogin(login: String, client: GitHubClient): UserResponse {
        val user = client.getUserWithLogin(login)
        UserQuery(database).create(user)
        return user
    }

    private fun updateUserLogin(userId: Long, newLogin: String, tokenUserId: Long) {
        database.transaction { tx ->
            tx.rebuildConflictUser(login = newLogin, tokenUserId = tokenUserId)
            UserQuery(using(tx)).update(id = userId, login = newLogin)
        }
    }

    private fun Configuration.rebuildConflictUser(login: String, tokenUserId: Long) {
        val conflictUser = UserQuery(using(this)).findBy(login = login)
        if (conflictUser != null) { // Lazily recreate it to avoid recursive conflict handling here
            UserQuery(using(this)).destroy(id = conflictUser.id)
            RepositoryQuery(using(this)).deleteAll(ownerId = conflictUser.id)
            UserUpdateJobQuery(using(this)).create(userId = conflictUser.id, tokenUserId = tokenUserId)
        }
    }

    // Concurrently executing `dao.acquireUntil` causes deadlock. So this executes it in a lock.
    private fun acquireUntil(conn: Connection, timeoutAt: Timestamp): Long {
        return DatabaseLock(database).withUserUpdateJobs {
            UserUpdateJobQuery(using(conn)).acquireUntil(timeoutAt)
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

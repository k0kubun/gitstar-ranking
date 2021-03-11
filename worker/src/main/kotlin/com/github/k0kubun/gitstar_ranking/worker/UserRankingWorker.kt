package com.github.k0kubun.gitstar_ranking.worker

import com.github.k0kubun.gitstar_ranking.config.Config
import com.github.k0kubun.gitstar_ranking.model.User
import java.util.concurrent.BlockingQueue
import org.skife.jdbi.v2.DBI
import kotlin.Throws
import java.lang.Exception
import java.util.concurrent.TimeUnit
import com.github.k0kubun.gitstar_ranking.model.UserRank
import com.github.k0kubun.gitstar_ranking.repository.dao.UserDao
import com.github.k0kubun.gitstar_ranking.repository.PaginatedUsers
import org.skife.jdbi.v2.TransactionStatus
import com.github.k0kubun.gitstar_ranking.repository.dao.UserRankDao
import java.util.ArrayList
import org.skife.jdbi.v2.Handle
import org.slf4j.LoggerFactory

class UserRankingWorker(config: Config) : Worker() {
    private val userRankingQueue: BlockingQueue<Boolean> = config.queueConfig.userRankingQueue
    private val dbi: DBI = DBI(config.databaseConfig.dataSource)

    // TODO: refactor the relationship between User/Organization/RepositoryRankingWorker
    private val organizationRankingWorker: OrganizationRankingWorker = OrganizationRankingWorker(config)
    private val repositoryRankingWorker: RepositoryRankingWorker = RepositoryRankingWorker(config)

    @Throws(Exception::class)
    override fun perform() {
        while (userRankingQueue.poll(5, TimeUnit.SECONDS) == null) {
            if (isStopped) {
                return
            }
        }
        LOG.info("----- started UserRankingWorker -----")
        dbi.open().use { handle ->
            val lastRank = updateUpperRanking(handle)
            lastRank?.let { updateLowerRanking(handle, it) }
        }
        LOG.info("----- finished UserRankingWorker -----")
        organizationRankingWorker.perform()
        repositoryRankingWorker.perform()
    }

    private fun updateUpperRanking(handle: Handle): UserRank? {
        val count = handle.attach(UserDao::class.java).countUsers() // warmup
        val paginatedUsers = PaginatedUsers(handle)
        var users: List<User>
        val commitPendingRanks: MutableList<UserRank> = ArrayList() // listed in stargazers_count DESC
        var currentRank: UserRank? = null
        var currentRankNum = 0
        while (!paginatedUsers.nextUsers().also { users = it }.isEmpty()) {
            // Shutdown immediately if requested, even if it's in progress.
            if (isStopped) {
                return null
            }
            for (user in users) {
                if (currentRank == null) {
                    currentRank = UserRank(user.stargazersCount, 1)
                    currentRankNum = 1
                } else if (currentRank.stargazersCount == user.stargazersCount) {
                    currentRankNum++
                } else {
                    commitPendingRanks.add(currentRank)
                    currentRank = UserRank(user.stargazersCount, currentRank.rank + currentRankNum)
                    currentRankNum = 1
                }
            }
            if (!commitPendingRanks.isEmpty()) {
                commitRanks(handle, commitPendingRanks)
                commitPendingRanks.clear()
            }
            val rows = currentRank!!.rank + currentRankNum - 1
            LOG.info("UserRankingWorker (" + calcProgress(rows, count) + ", " + Integer.valueOf(rows).toString() +
                "/" + Integer.valueOf(count).toString() + " rows, rank " + Integer.valueOf(currentRank.rank).toString() + ", " +
                Integer.valueOf(currentRank.stargazersCount).toString() + " stars)")

            // Switch the way to calculate ranking under 10 stars
            if (currentRank.stargazersCount <= ITERATE_MIN_STARS) {
                return currentRank
            }
        }
        return currentRank
    }

    private fun updateLowerRanking(handle: Handle, lastUserRank: UserRank) {
        val userRanks: MutableList<UserRank> = ArrayList() // listed in stargazers_count DESC
        userRanks.add(lastUserRank)
        var lastRank = lastUserRank.rank
        for (lastStars in lastUserRank.stargazersCount downTo 1) {
            LOG.info("UserRankingWorker for " + Integer.valueOf(lastStars - 1).toString())
            val count = handle.attach(UserDao::class.java).countUsersHavingStars(lastStars)
            userRanks.add(UserRank(lastStars - 1, lastRank + count))
            lastRank += count
        }
        commitRanks(handle, userRanks)
    }

    private fun commitRanks(handle: Handle, userRanks: List<UserRank>) {
        // TODO: test this
        // `userRanks` is listed in stargazers_count DESC
        val maxStars = userRanks[0].stargazersCount
        val highestRank = userRanks[0].rank
        val minStars = lastOf(userRanks).stargazersCount
        val lowestRank = lastOf(userRanks).rank
        handle.useTransaction { conn: Handle, status: TransactionStatus? ->
            conn.attach(UserRankDao::class.java).deleteStarsBetween(minStars, maxStars)
            conn.attach(UserRankDao::class.java).deleteRankBetween(highestRank, lowestRank)
            conn.attach(UserRankDao::class.java).bulkInsert(userRanks)
        }
    }

    private fun lastOf(userRanks: List<UserRank>): UserRank {
        return userRanks[userRanks.size - 1]
    }

    private fun calcProgress(child: Int, parent: Int): String {
        return String.format("%.3f%%", child.toFloat() / parent.toFloat())
    }

    companion object {
        private const val ITERATE_MIN_STARS = 10
        private val LOG = LoggerFactory.getLogger(UserRankingWorker::class.java)
    }
}

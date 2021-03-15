package com.github.k0kubun.gitstar_ranking.workers

import com.github.k0kubun.gitstar_ranking.GitstarRankingConfiguration
import com.github.k0kubun.gitstar_ranking.core.UserRank
import com.github.k0kubun.gitstar_ranking.db.PaginatedUsers
import com.github.k0kubun.gitstar_ranking.db.UserQuery
import com.github.k0kubun.gitstar_ranking.db.UserRankQuery
import java.util.ArrayList
import org.jooq.impl.DSL.using
import org.slf4j.LoggerFactory

private const val ITERATE_MIN_STARS = 10

class UserRankingWorker(config: GitstarRankingConfiguration) : Worker() {
    private val logger = LoggerFactory.getLogger(UserRankingWorker::class.simpleName)
    private val database = config.database.dslContext

    override fun perform() {
        logger.info("----- started UserRankingWorker -----")
        val lastRank = updateUpperRanking() // stars > 10
        lastRank?.let { updateLowerRanking(it) }
        logger.info("----- finished UserRankingWorker -----")
    }

    private fun updateUpperRanking(): UserRank? {
        val count = UserQuery(database).count() // warmup
        val paginatedUsers = PaginatedUsers(database)
        val commitPendingRanks: MutableList<UserRank> = ArrayList() // listed in stargazers_count DESC
        var currentRank: UserRank? = null
        var currentRankNum = 0
        while (true) {
            val users = paginatedUsers.nextUsers()
            if (users.isEmpty()) break

            // Shutdown immediately if requested, even if it's in progress.
            if (isStopped) {
                return null
            }
            for (user in users) {
                when {
                    currentRank == null -> {
                        currentRank = UserRank(user.stargazersCount, 1)
                        currentRankNum = 1
                    }
                    currentRank.stargazersCount == user.stargazersCount -> {
                        currentRankNum++
                    }
                    else -> {
                        commitPendingRanks.add(currentRank)
                        currentRank = UserRank(user.stargazersCount, currentRank.rank + currentRankNum)
                        currentRankNum = 1
                    }
                }
            }
            if (commitPendingRanks.isNotEmpty()) {
                commitRanks(commitPendingRanks)
                commitPendingRanks.clear()
            }
            val rows = currentRank!!.rank + currentRankNum - 1
            logger.info("UserRankingWorker (${calcProgress(rows, count)}, $rows/$count rows, " +
                "rank ${currentRank.rank}, ${currentRank.stargazersCount} stars)")

            // Switch the way to calculate ranking under 10 stars
            if (currentRank.stargazersCount <= ITERATE_MIN_STARS) {
                return currentRank
            }
        }
        return currentRank
    }

    private fun updateLowerRanking(lastUserRank: UserRank) {
        val userRanks = mutableListOf(lastUserRank) // listed in stargazers_count DESC
        var lastRank = lastUserRank.rank
        for (lastStars in lastUserRank.stargazersCount downTo 1) {
            logger.info("UserRankingWorker for ${lastStars - 1}")
            val count = UserQuery(database).count(stars = lastStars)
            userRanks.add(UserRank(lastStars - 1, lastRank + count))
            lastRank += count
        }
        commitRanks(userRanks)
    }

    private fun commitRanks(ranks: List<UserRank>) {
        // `ranks` is listed in stargazers_count DESC. TODO: test this
        val maxStars = ranks[0].stargazersCount
        val bestRank = ranks[0].rank
        val minStars = lastOf(ranks).stargazersCount
        val worstRank = lastOf(ranks).rank
        database.transaction { tx ->
            UserRankQuery(using(tx)).deleteByStars(min = minStars, max = maxStars)
            UserRankQuery(using(tx)).deleteByRank(min = bestRank, max = worstRank)
            UserRankQuery(using(tx)).insertAll(ranks)
        }
    }

    private fun lastOf(userRanks: List<UserRank>): UserRank {
        return userRanks[userRanks.size - 1]
    }

    private fun calcProgress(child: Long, parent: Long): String {
        return String.format("%.3f%%", child.toFloat() / parent.toFloat())
    }
}

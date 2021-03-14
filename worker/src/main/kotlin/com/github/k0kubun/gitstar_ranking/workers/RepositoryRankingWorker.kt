package com.github.k0kubun.gitstar_ranking.workers

import com.github.k0kubun.gitstar_ranking.GitstarRankingConfiguration
import com.github.k0kubun.gitstar_ranking.core.Repository
import com.github.k0kubun.gitstar_ranking.core.RepositoryRank
import com.github.k0kubun.gitstar_ranking.db.PaginatedRepositories
import com.github.k0kubun.gitstar_ranking.db.RepositoryDao
import com.github.k0kubun.gitstar_ranking.db.RepositoryRankDao
import java.util.ArrayList
import org.skife.jdbi.v2.DBI
import org.skife.jdbi.v2.Handle
import org.skife.jdbi.v2.TransactionStatus
import org.slf4j.LoggerFactory

private const val ITERATE_MIN_STARS = 10

class RepositoryRankingWorker(config: GitstarRankingConfiguration) : Worker() {
    private val logger = LoggerFactory.getLogger(RepositoryRankingWorker::class.simpleName)
    private val dbi: DBI = DBI(config.database.dataSource)

    override fun perform() {
        logger.info("----- started RepositoryRankingWorker -----")
        dbi.open().use { handle ->
            val lastRank = updateUpperRanking(handle)
            lastRank?.let { updateLowerRanking(handle, it) }
        }
        logger.info("----- finished RepositoryRankingWorker -----")
    }

    private fun updateUpperRanking(handle: Handle): RepositoryRank? {
        val count = handle.attach(RepositoryDao::class.java).countRepos() // warmup
        val paginatedRepos = PaginatedRepositories(handle)
        var repos: List<Repository>
        val commitPendingRanks: MutableList<RepositoryRank> = ArrayList() // listed in stargazers_count DESC
        var currentRank: RepositoryRank? = null
        var currentRankNum = 0
        while (paginatedRepos.nextRepos().also { repos = it }.isNotEmpty()) {
            // Shutdown immediately if requested, even if it's in progress.
            if (isStopped) {
                return null
            }
            for (repo in repos) {
                if (currentRank == null) {
                    currentRank = RepositoryRank(repo.stargazersCount, 1)
                    currentRankNum = 1
                } else if (currentRank.stargazersCount == repo.stargazersCount) {
                    currentRankNum++
                } else {
                    commitPendingRanks.add(currentRank)
                    currentRank = RepositoryRank(repo.stargazersCount, currentRank.rank + currentRankNum)
                    currentRankNum = 1
                }
            }
            if (commitPendingRanks.isNotEmpty()) {
                commitRanks(handle, commitPendingRanks)
                commitPendingRanks.clear()
            }
            val rows = currentRank!!.rank + currentRankNum - 1
            logger.info("RepositoryRankingWorker (${calcProgress(rows, count)}, $rows/$count rows, " +
                "rank ${currentRank.rank}, ${currentRank.stargazersCount} stars)")

            // Switch the way to calculate ranking under 10 stars
            if (currentRank.stargazersCount <= ITERATE_MIN_STARS) {
                return currentRank
            }
        }
        return currentRank
    }

    private fun updateLowerRanking(handle: Handle, lastRepoRank: RepositoryRank) {
        val repoRanks: MutableList<RepositoryRank> = ArrayList() // listed in stargazers_count DESC
        repoRanks.add(lastRepoRank)
        var lastRank = lastRepoRank.rank
        (lastRepoRank.stargazersCount downTo 1).forEach { lastStars ->
            logger.info("RepositoryRankingWorker for ${lastStars - 1}")
            val count = handle.attach(RepositoryDao::class.java).countReposHavingStars(lastStars)
            repoRanks.add(RepositoryRank(lastStars - 1, lastRank + count))
            lastRank += count
        }
        commitRanks(handle, repoRanks)
    }

    private fun commitRanks(handle: Handle, repoRanks: List<RepositoryRank>) {
        // `repoRanks` is listed in stargazers_count DESC
        val maxStars = repoRanks[0].stargazersCount
        val highestRank = repoRanks[0].rank
        val minStars = lastOf(repoRanks).stargazersCount
        val lowestRank = lastOf(repoRanks).rank
        handle.useTransaction { conn: Handle, _: TransactionStatus? ->
            conn.attach(RepositoryRankDao::class.java).deleteStarsBetween(minStars, maxStars)
            conn.attach(RepositoryRankDao::class.java).deleteRankBetween(highestRank, lowestRank)
            conn.attach(RepositoryRankDao::class.java).bulkInsert(repoRanks)
        }
    }

    private fun lastOf(repoRanks: List<RepositoryRank>): RepositoryRank {
        return repoRanks[repoRanks.size - 1]
    }

    private fun calcProgress(child: Int, parent: Int): String {
        return String.format("%.3f%%", child.toFloat() / parent.toFloat())
    }
}

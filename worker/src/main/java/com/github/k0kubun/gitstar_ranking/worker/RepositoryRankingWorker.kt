package com.github.k0kubun.gitstar_ranking.worker

import com.github.k0kubun.gitstar_ranking.config.Config
import com.github.k0kubun.gitstar_ranking.model.Repository
import java.util.concurrent.BlockingQueue
import org.skife.jdbi.v2.DBI
import kotlin.Throws
import java.lang.Exception
import com.github.k0kubun.gitstar_ranking.model.RepositoryRank
import com.github.k0kubun.gitstar_ranking.repository.dao.RepositoryDao
import org.skife.jdbi.v2.TransactionStatus
import com.github.k0kubun.gitstar_ranking.repository.dao.RepositoryRankDao
import java.util.ArrayList
import org.skife.jdbi.v2.Handle
import org.slf4j.LoggerFactory

private const val PAGE_SIZE = 5000

class RepositoryRankingWorker(config: Config) : Worker() {
    private val repoRankingQueue: BlockingQueue<Boolean>
    private val dbi: DBI
    @Throws(Exception::class)
    override fun perform() {
        LOG.info("----- started RepositoryRankingWorker -----")
        dbi.open().use { handle ->
            val lastRank = updateUpperRanking(handle)
            lastRank?.let { updateLowerRanking(handle, it) }
        }
        LOG.info("----- finished RepositoryRankingWorker -----")
    }

    private fun updateUpperRanking(handle: Handle): RepositoryRank? {
        val count = handle.attach(RepositoryDao::class.java).countRepos() // warmup
        val paginatedRepos = PaginatedRepositories(handle)
        var repos: List<Repository>
        val commitPendingRanks: MutableList<RepositoryRank> = ArrayList() // listed in stargazers_count DESC
        var currentRank: RepositoryRank? = null
        var currentRankNum = 0
        while (!paginatedRepos.nextRepos().also { repos = it }.isEmpty()) {
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
            if (!commitPendingRanks.isEmpty()) {
                commitRanks(handle, commitPendingRanks)
                commitPendingRanks.clear()
            }
            val rows = currentRank!!.rank + currentRankNum - 1
            LOG.info("RepositoryRankingWorker (" + calcProgress(rows, count) + ", " + Integer.valueOf(rows).toString() +
                "/" + Integer.valueOf(count).toString() + " rows, rank " + Integer.valueOf(currentRank.rank).toString() + ", " +
                Integer.valueOf(currentRank.stargazersCount).toString() + " stars)")

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
        for (lastStars in lastRepoRank.stargazersCount downTo 1) {
            LOG.info("RepositoryRankingWorker for " + Integer.valueOf(lastStars - 1).toString())
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
        handle.useTransaction { conn: Handle, status: TransactionStatus? ->
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

    // This class does cursor-based-pagination for repositories order by stargazers_count DESC.
    inner class PaginatedRepositories(handle: Handle) {
        private val repoDao: RepositoryDao
        private var lastMinStars: Int?
        private var lastMinId: Long?
        fun nextRepos(): List<Repository> {
            val repos: List<Repository>
            repos = if (lastMinId == null && lastMinStars == null) {
                repoDao.starsDescFirstRepos(PAGE_SIZE)
            } else {
                repoDao.starsDescReposAfter(lastMinStars, lastMinId, PAGE_SIZE)
            }
            if (repos.isEmpty()) {
                return repos
            }
            val lastRepo = repos[repos.size - 1]
            lastMinStars = lastRepo.stargazersCount
            lastMinId = lastRepo.id
            return repos
        }

        init {
            repoDao = handle.attach(RepositoryDao::class.java)
            lastMinStars = null
            lastMinId = null
        }
    }

    companion object {
        private const val ITERATE_MIN_STARS = 10
        private val LOG = LoggerFactory.getLogger(RepositoryRankingWorker::class.java)
    }

    init {
        repoRankingQueue = config.queueConfig.repoRankingQueue
        dbi = DBI(config.databaseConfig.dataSource)
    }
}

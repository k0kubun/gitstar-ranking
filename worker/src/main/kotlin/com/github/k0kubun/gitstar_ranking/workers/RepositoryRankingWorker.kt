package com.github.k0kubun.gitstar_ranking.workers

import com.github.k0kubun.gitstar_ranking.GitstarRankingConfiguration
import com.github.k0kubun.gitstar_ranking.core.RepositoryRank
import com.github.k0kubun.gitstar_ranking.db.PaginatedRepositories
import com.github.k0kubun.gitstar_ranking.db.RepositoryQuery
import com.github.k0kubun.gitstar_ranking.db.RepositoryRankQuery
import java.util.ArrayList
import org.jooq.impl.DSL.using
import org.slf4j.LoggerFactory

private const val ITERATE_MIN_STARS = 10

class RepositoryRankingWorker(config: GitstarRankingConfiguration) : Worker() {
    private val logger = LoggerFactory.getLogger(RepositoryRankingWorker::class.simpleName)
    private val database = config.database.dslContext

    override fun perform() {
        logger.info("----- started RepositoryRankingWorker -----")
        val lastRank = updateUpperRanking() // stars > 10
        lastRank?.let { updateLowerRanking(it) }
        logger.info("----- finished RepositoryRankingWorker -----")
    }

    private fun updateUpperRanking(): RepositoryRank? {
        val count = RepositoryQuery(database).count() // warmup
        val paginatedRepos = PaginatedRepositories(database)
        val commitPendingRanks: MutableList<RepositoryRank> = ArrayList() // listed in stargazers_count DESC
        var currentRank: RepositoryRank? = null
        var currentRankNum = 0
        while (true) {
            val repos = paginatedRepos.nextRepos()
            if (repos.isEmpty()) break

            // Shutdown immediately if requested, even if it's in progress.
            if (isStopped) {
                return null
            }
            for (repo in repos) {
                when {
                    currentRank == null -> {
                        currentRank = RepositoryRank(repo.stargazersCount, 1)
                        currentRankNum = 1
                    }
                    currentRank.stargazersCount == repo.stargazersCount -> {
                        currentRankNum++
                    }
                    else -> {
                        commitPendingRanks.add(currentRank)
                        currentRank = RepositoryRank(repo.stargazersCount, currentRank.rank + currentRankNum)
                        currentRankNum = 1
                    }
                }
            }
            if (commitPendingRanks.isNotEmpty()) {
                commitRanks(commitPendingRanks)
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

    private fun updateLowerRanking(lastRepoRank: RepositoryRank) {
        val repoRanks: MutableList<RepositoryRank> = ArrayList() // listed in stargazers_count DESC
        repoRanks.add(lastRepoRank)
        var lastRank = lastRepoRank.rank
        (lastRepoRank.stargazersCount downTo 1).forEach { lastStars ->
            logger.info("RepositoryRankingWorker for ${lastStars - 1}")
            val count = RepositoryQuery(database).count(stars = lastStars)
            repoRanks.add(RepositoryRank(lastStars - 1, lastRank + count))
            lastRank += count
        }
        commitRanks(repoRanks)
    }

    private fun commitRanks(ranks: List<RepositoryRank>) {
        // `ranks` is listed in stargazers_count DESC
        val maxStars = ranks[0].stargazersCount
        val bestRank = ranks[0].rank
        val minStars = lastOf(ranks).stargazersCount
        val worstRank = lastOf(ranks).rank
        database.transaction { tx ->
            RepositoryRankQuery(using(tx)).deleteByStars(min = minStars, max = maxStars)
            RepositoryRankQuery(using(tx)).deleteByRank(min = bestRank, max = worstRank)
            RepositoryRankQuery(using(tx)).insertAll(ranks)
        }
    }

    private fun lastOf(repoRanks: List<RepositoryRank>): RepositoryRank {
        return repoRanks[repoRanks.size - 1]
    }

    private fun calcProgress(child: Long, parent: Long): String {
        return String.format("%.3f%%", child.toFloat() / parent.toFloat())
    }
}

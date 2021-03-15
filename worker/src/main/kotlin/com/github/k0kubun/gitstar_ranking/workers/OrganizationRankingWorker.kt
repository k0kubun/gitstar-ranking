package com.github.k0kubun.gitstar_ranking.workers

import com.github.k0kubun.gitstar_ranking.GitstarRankingConfiguration
import com.github.k0kubun.gitstar_ranking.core.OrganizationRank
import com.github.k0kubun.gitstar_ranking.db.OrganizationQuery
import com.github.k0kubun.gitstar_ranking.db.OrganizationRankQuery
import com.github.k0kubun.gitstar_ranking.db.PaginatedOrganizations
import org.jooq.impl.DSL.using
import org.slf4j.LoggerFactory

private const val ITERATE_MIN_STARS = 10

class OrganizationRankingWorker(config: GitstarRankingConfiguration) : Worker() {
    private val logger = LoggerFactory.getLogger(OrganizationRankingWorker::class.simpleName)
    private val database = config.database.dslContext

    override fun perform() {
        logger.info("----- started OrganizationRankingWorker -----")
        val lastRank = updateUpperRanking() // stars > 10
        lastRank?.let { updateLowerRanking(it) }
        logger.info("----- finished OrganizationRankingWorker -----")
    }

    private fun updateUpperRanking(): OrganizationRank? {
        val count = OrganizationQuery(database).count() // warmup
        val paginatedOrgs = PaginatedOrganizations(database)
        val commitPendingRanks = mutableListOf<OrganizationRank>() // listed in stargazers_count DESC
        var currentRank: OrganizationRank? = null
        var currentRankNum = 0
        while (true) {
            val orgs = paginatedOrgs.nextOrgs()
            if (orgs.isEmpty()) break

            // Shutdown immediately if requested, even if it's in progress.
            if (isStopped) {
                return null
            }
            for (org in orgs) {
                when {
                    currentRank == null -> {
                        currentRank = OrganizationRank(org.stargazersCount, 1)
                        currentRankNum = 1
                    }
                    currentRank.stargazersCount == org.stargazersCount -> {
                        currentRankNum++
                    }
                    else -> {
                        commitPendingRanks.add(currentRank)
                        currentRank = OrganizationRank(org.stargazersCount, currentRank.rank + currentRankNum)
                        currentRankNum = 1
                    }
                }
            }
            if (commitPendingRanks.isNotEmpty()) {
                commitRanks(commitPendingRanks)
                commitPendingRanks.clear()
            }
            val rows = currentRank!!.rank + currentRankNum - 1
            logger.info("OrganizationRankingWorker (${calcProgress(rows, count)}, " +
                "$rows/$count rows, rank ${currentRank.rank}, ${currentRank.stargazersCount} stars)")

            // Switch the way to calculate ranking under 10 stars
            if (currentRank.stargazersCount <= ITERATE_MIN_STARS) {
                return currentRank
            }
        }
        return currentRank
    }

    private fun updateLowerRanking(lastOrgRank: OrganizationRank) {
        val orgRanks = mutableListOf(lastOrgRank) // listed in stargazers_count DESC
        var lastRank = lastOrgRank.rank
        for (lastStars in lastOrgRank.stargazersCount downTo 1) {
            logger.info("OrganizationRankingWorker for ${lastStars - 1}")
            val count = OrganizationQuery(database).count(stars = lastStars)
            orgRanks.add(OrganizationRank(lastStars - 1, lastRank + count))
            lastRank += count
        }
        commitRanks(orgRanks)
    }

    private fun commitRanks(ranks: List<OrganizationRank>) {
        // `ranks` is listed in stargazers_count DESC
        val maxStars = ranks[0].stargazersCount
        val bestRank = ranks[0].rank
        val minStars = lastOf(ranks).stargazersCount
        val worstRank = lastOf(ranks).rank
        database.transaction { tx ->
            OrganizationRankQuery(using(tx)).deleteByStars(min = minStars, max = maxStars)
            OrganizationRankQuery(using(tx)).deleteByRank(min = bestRank, max = worstRank)
            OrganizationRankQuery(using(tx)).insertAll(ranks)
        }
    }

    private fun lastOf(orgRanks: List<OrganizationRank>): OrganizationRank {
        return orgRanks[orgRanks.size - 1]
    }

    private fun calcProgress(child: Long, parent: Long): String {
        return String.format("%.3f%%", child.toFloat() / parent.toFloat())
    }
}

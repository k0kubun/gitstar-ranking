package com.github.k0kubun.gitstar_ranking.workers

import com.github.k0kubun.gitstar_ranking.GitstarRankingConfiguration
import com.github.k0kubun.gitstar_ranking.core.Organization
import com.github.k0kubun.gitstar_ranking.core.OrganizationRank
import com.github.k0kubun.gitstar_ranking.db.OrganizationDao
import com.github.k0kubun.gitstar_ranking.db.OrganizationRankDao
import com.github.k0kubun.gitstar_ranking.db.PaginatedOrganizations
import java.util.ArrayList
import org.skife.jdbi.v2.DBI
import org.skife.jdbi.v2.Handle
import org.skife.jdbi.v2.TransactionStatus
import org.slf4j.LoggerFactory

private const val ITERATE_MIN_STARS = 10

class OrganizationRankingWorker(config: GitstarRankingConfiguration) : Worker() {
    private val logger = LoggerFactory.getLogger(OrganizationRankingWorker::class.simpleName)
    private val dbi: DBI = DBI(config.database.dataSource)

    override fun perform() {
        logger.info("----- started OrganizationRankingWorker -----")
        dbi.open().use { handle ->
            val lastRank = updateUpperRanking(handle)
            lastRank?.let { updateLowerRanking(handle, it) }
        }
        logger.info("----- finished OrganizationRankingWorker -----")
    }

    private fun updateUpperRanking(handle: Handle): OrganizationRank? {
        val count = handle.attach(OrganizationDao::class.java).countOrganizations() // warmup
        val paginatedOrgs = PaginatedOrganizations(handle)
        var orgs: List<Organization>
        val commitPendingRanks: MutableList<OrganizationRank> = ArrayList() // listed in stargazers_count DESC
        var currentRank: OrganizationRank? = null
        var currentRankNum = 0
        while (paginatedOrgs.nextOrgs().also { orgs = it }.isNotEmpty()) {
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
                commitRanks(handle, commitPendingRanks)
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

    private fun updateLowerRanking(handle: Handle, lastOrgRank: OrganizationRank) {
        val orgRanks: MutableList<OrganizationRank> = ArrayList() // listed in stargazers_count DESC
        orgRanks.add(lastOrgRank)
        var lastRank = lastOrgRank.rank
        for (lastStars in lastOrgRank.stargazersCount downTo 1) {
            logger.info("OrganizationRankingWorker for ${lastStars - 1}")
            val count = handle.attach(OrganizationDao::class.java).countOrganizationsHavingStars(lastStars)
            orgRanks.add(OrganizationRank(lastStars - 1, lastRank + count))
            lastRank += count
        }
        commitRanks(handle, orgRanks)
    }

    private fun commitRanks(handle: Handle, orgRanks: List<OrganizationRank>) {
        // `orgRanks` is listed in stargazers_count DESC
        val maxStars = orgRanks[0].stargazersCount
        val highestRank = orgRanks[0].rank
        val minStars = lastOf(orgRanks).stargazersCount
        val lowestRank = lastOf(orgRanks).rank
        handle.useTransaction { conn: Handle, _: TransactionStatus? ->
            conn.attach(OrganizationRankDao::class.java).deleteStarsBetween(minStars, maxStars)
            conn.attach(OrganizationRankDao::class.java).deleteRankBetween(highestRank, lowestRank)
            conn.attach(OrganizationRankDao::class.java).bulkInsert(orgRanks)
        }
    }

    private fun lastOf(orgRanks: List<OrganizationRank>): OrganizationRank {
        return orgRanks[orgRanks.size - 1]
    }

    private fun calcProgress(child: Int, parent: Int): String {
        return String.format("%.3f%%", child.toFloat() / parent.toFloat())
    }
}

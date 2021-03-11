package com.github.k0kubun.gitstar_ranking.worker

import com.github.k0kubun.gitstar_ranking.config.Config
import java.util.concurrent.BlockingQueue
import org.skife.jdbi.v2.DBI
import kotlin.Throws
import java.lang.Exception
import com.github.k0kubun.gitstar_ranking.model.OrganizationRank
import com.github.k0kubun.gitstar_ranking.repository.dao.OrganizationDao
import com.github.k0kubun.gitstar_ranking.repository.PaginatedOrganizations
import com.github.k0kubun.gitstar_ranking.model.Organization
import org.skife.jdbi.v2.TransactionStatus
import com.github.k0kubun.gitstar_ranking.repository.dao.OrganizationRankDao
import java.util.ArrayList
import org.skife.jdbi.v2.Handle
import org.slf4j.LoggerFactory

class OrganizationRankingWorker(config: Config) : Worker() {
    private val dbi: DBI
    @Throws(Exception::class)
    override fun perform() {
        LOG.info("----- started OrganizationRankingWorker -----")
        dbi.open().use { handle ->
            val lastRank = updateUpperRanking(handle)
            lastRank?.let { updateLowerRanking(handle, it) }
        }
        LOG.info("----- finished OrganizationRankingWorker -----")
    }

    private fun updateUpperRanking(handle: Handle): OrganizationRank? {
        val count = handle.attach(OrganizationDao::class.java).countOrganizations() // warmup
        val paginatedOrgs = PaginatedOrganizations(handle)
        var orgs: List<Organization>
        val commitPendingRanks: MutableList<OrganizationRank> = ArrayList() // listed in stargazers_count DESC
        var currentRank: OrganizationRank? = null
        var currentRankNum = 0
        while (!paginatedOrgs.nextOrgs().also { orgs = it }.isEmpty()) {
            // Shutdown immediately if requested, even if it's in progress.
            if (isStopped) {
                return null
            }
            for (org in orgs) {
                if (currentRank == null) {
                    currentRank = OrganizationRank(org.stargazersCount, 1)
                    currentRankNum = 1
                } else if (currentRank.stargazersCount == org.stargazersCount) {
                    currentRankNum++
                } else {
                    commitPendingRanks.add(currentRank)
                    currentRank = OrganizationRank(org.stargazersCount, currentRank.rank + currentRankNum)
                    currentRankNum = 1
                }
            }
            if (!commitPendingRanks.isEmpty()) {
                commitRanks(handle, commitPendingRanks)
                commitPendingRanks.clear()
            }
            val rows = currentRank!!.rank + currentRankNum - 1
            LOG.info("OrganizationRankingWorker (" + calcProgress(rows, count) + ", " + Integer.valueOf(rows).toString() +
                "/" + Integer.valueOf(count).toString() + " rows, rank " + Integer.valueOf(currentRank.rank).toString() + ", " +
                Integer.valueOf(currentRank.stargazersCount).toString() + " stars)")

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
            LOG.info("OrganizationRankingWorker for " + Integer.valueOf(lastStars - 1).toString())
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
        handle.useTransaction { conn: Handle, status: TransactionStatus? ->
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

    companion object {
        private const val ITERATE_MIN_STARS = 10
        private val LOG = LoggerFactory.getLogger(OrganizationRankingWorker::class.java)
    }

    init {
        dbi = DBI(config.databaseConfig.dataSource)
    }
}

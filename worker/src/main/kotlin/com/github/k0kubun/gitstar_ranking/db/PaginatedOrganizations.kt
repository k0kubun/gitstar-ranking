package com.github.k0kubun.gitstar_ranking.db

import com.github.k0kubun.gitstar_ranking.core.Organization
import org.skife.jdbi.v2.Handle

private const val PAGE_SIZE = 5000

// This class does cursor-based-pagination for organizations order by stargazers_count DESC.
class PaginatedOrganizations(handle: Handle) {
    private val orgDao: OrganizationDao = handle.attach(OrganizationDao::class.java)
    private var lastMinStars: Int? = null
    private var lastMinId: Int? = null

    fun nextOrgs(): List<Organization> {
        val orgs: List<Organization> = if (lastMinId == null && lastMinStars == null) {
            orgDao.starsDescFirstOrgs(PAGE_SIZE)
        } else {
            orgDao.starsDescOrgsAfter(lastMinStars, lastMinId, PAGE_SIZE)
        }
        if (orgs.isEmpty()) {
            return orgs
        }
        val lastOrg = orgs[orgs.size - 1]
        lastMinStars = lastOrg.stargazersCount
        lastMinId = lastOrg.id
        return orgs
    }
}

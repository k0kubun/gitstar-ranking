package com.github.k0kubun.gitstar_ranking.db

import com.github.k0kubun.gitstar_ranking.core.Organization
import com.github.k0kubun.gitstar_ranking.core.StarsCursor
import org.jooq.DSLContext

private const val PAGE_SIZE = 5000

// This class does cursor-based-pagination for organizations order by stargazers_count DESC.
class PaginatedOrganizations(private val database: DSLContext) {
    private var lastMinStars: Long? = null
    private var lastMinId: Long? = null

    fun nextOrgs(): List<Organization> {
        val orgs: List<Organization> = if (lastMinId != null && lastMinStars != null) {
            OrganizationQuery(database).orderByStarsDesc(
                limit = PAGE_SIZE,
                after = StarsCursor(id = lastMinId!!, stars = lastMinStars!!)
            )
        } else {
            OrganizationQuery(database).orderByStarsDesc(limit = PAGE_SIZE)
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

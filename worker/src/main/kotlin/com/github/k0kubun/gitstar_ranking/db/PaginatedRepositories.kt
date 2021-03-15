package com.github.k0kubun.gitstar_ranking.db

import com.github.k0kubun.gitstar_ranking.core.Repository
import com.github.k0kubun.gitstar_ranking.core.StarsCursor
import org.jooq.DSLContext

private const val PAGE_SIZE = 5000

// This class does cursor-based-pagination for repositories order by stargazers_count DESC.
class PaginatedRepositories(private val database: DSLContext) {
    private var lastMinStars: Long? = null
    private var lastMinId: Long? = null

    fun nextRepos(): List<Repository> {
        val repos = if (lastMinId != null && lastMinStars != null) {
            RepositoryQuery(database).orderByStarsDesc(
                limit = PAGE_SIZE,
                after = StarsCursor(id = lastMinId!!, stars = lastMinStars!!)
            )
        } else {
            RepositoryQuery(database).orderByStarsDesc(limit = PAGE_SIZE)
        }
        if (repos.isEmpty()) {
            return repos
        }
        val lastRepo = repos[repos.size - 1]
        lastMinStars = lastRepo.stargazersCount
        lastMinId = lastRepo.id
        return repos
    }
}

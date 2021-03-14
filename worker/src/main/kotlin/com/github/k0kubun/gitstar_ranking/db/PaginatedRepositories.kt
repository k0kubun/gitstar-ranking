package com.github.k0kubun.gitstar_ranking.db

import com.github.k0kubun.gitstar_ranking.core.Repository
import org.skife.jdbi.v2.Handle

private const val PAGE_SIZE = 5000

// This class does cursor-based-pagination for repositories order by stargazers_count DESC.
class PaginatedRepositories(handle: Handle) {
    private val repoDao: RepositoryDao = handle.attach(RepositoryDao::class.java)
    private var lastMinStars: Long? = null
    private var lastMinId: Long? = null

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
}

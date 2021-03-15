package com.github.k0kubun.gitstar_ranking.db

import com.github.k0kubun.gitstar_ranking.core.StarsCursor
import com.github.k0kubun.gitstar_ranking.core.User
import org.jooq.DSLContext

private const val PAGE_SIZE = 5000

// This class does cursor-based-pagination for users order by stargazers_count DESC.
class PaginatedUsers(private val database: DSLContext) {
    private var lastMinStars: Long? = null
    private var lastMinId: Long? = null

    fun nextUsers(): List<User> {
        val users = if (lastMinId != null && lastMinStars != null) {
            UserQuery(database).orderByStarsDesc(
                limit = PAGE_SIZE,
                after = StarsCursor(id = lastMinId!!, stars = lastMinStars!!),
            )
        } else {
            UserQuery(database).orderByStarsDesc(limit = PAGE_SIZE)
        }
        if (users.isEmpty()) {
            return users
        }
        val lastUser = users[users.size - 1]
        lastMinStars = lastUser.stargazersCount
        lastMinId = lastUser.id
        return users
    }
}

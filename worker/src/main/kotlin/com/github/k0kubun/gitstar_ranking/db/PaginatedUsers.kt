package com.github.k0kubun.gitstar_ranking.db

import com.github.k0kubun.gitstar_ranking.core.User
import org.skife.jdbi.v2.Handle

private const val PAGE_SIZE = 5000

// This class does cursor-based-pagination for users order by stargazers_count DESC.
class PaginatedUsers(handle: Handle) {
    private val userDao: UserDao = handle.attach(UserDao::class.java)
    private var lastMinStars: Int? = null
    private var lastMinId: Long? = null

    fun nextUsers(): List<User> {
        val users: List<User> = if (lastMinId == null && lastMinStars == null) {
            userDao.starsDescFirstUsers(PAGE_SIZE)
        } else {
            userDao.starsDescUsersAfter(lastMinStars, lastMinId, PAGE_SIZE)
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

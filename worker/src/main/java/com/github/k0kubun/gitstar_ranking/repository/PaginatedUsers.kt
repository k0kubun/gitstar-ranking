package com.github.k0kubun.gitstar_ranking.repository

import com.github.k0kubun.gitstar_ranking.model.User
import com.github.k0kubun.gitstar_ranking.repository.dao.UserDao
import org.skife.jdbi.v2.Handle

// This class does cursor-based-pagination for users order by stargazers_count DESC.
class PaginatedUsers(handle: Handle) {
    private val userDao: UserDao = handle.attach(UserDao::class.java)
    private var lastMinStars: Int?
    private var lastMinId: Long?
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

    companion object {
        private const val PAGE_SIZE = 5000
    }

    init {
        lastMinStars = null
        lastMinId = null
    }
}

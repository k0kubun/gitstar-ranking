package com.github.k0kubun.github_ranking.repository;

import com.github.k0kubun.github_ranking.repository.dao.UserDao;
import com.github.k0kubun.github_ranking.model.User;

import java.util.List;

import org.skife.jdbi.v2.Handle;

// This class does cursor-based-pagination for users order by stargazers_count DESC.
public class PaginatedUsers
{
    private static final int PAGE_SIZE = 5000;

    private final UserDao userDao;
    private Integer lastMinStars;
    private Integer lastMinId;

    public PaginatedUsers(Handle handle)
    {
        userDao = handle.attach(UserDao.class);
        lastMinStars = null;
        lastMinId = null;
    }

    public List<User> nextUsers()
    {
        List<User> users;
        if (lastMinId == null && lastMinStars == null) {
            users = userDao.starsDescFirstUsers(PAGE_SIZE);
        }
        else {
            users = userDao.starsDescUsersAfter(lastMinStars, lastMinId, PAGE_SIZE);
        }
        if (users.isEmpty()) {
            return users;
        }

        User lastUser = users.get(users.size() - 1);
        lastMinStars = lastUser.getStargazersCount();
        lastMinId = lastUser.getId();
        return users;
    }
}

package com.github.k0kubun.github_ranking.worker;

import com.github.k0kubun.github_ranking.config.Config;
import com.github.k0kubun.github_ranking.dao.UserDao;
import com.github.k0kubun.github_ranking.model.User;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserRankingWorker extends Worker
{
    private static final Logger LOG = LoggerFactory.getLogger(UserRankingWorker.class);

    private final BlockingQueue<Boolean> userRankingQueue;
    private final DBI dbi;

    public UserRankingWorker(Config config)
    {
        super();
        userRankingQueue = config.getQueueConfig().getUserRankingQueue();
        dbi = new DBI(config.getDatabaseConfig().getDataSource());
    }

    @Override
    public void perform() throws Exception
    {
        while (userRankingQueue.poll(5, TimeUnit.SECONDS) == null) {
            if (isStopped) {
                return;
            }
        }
        LOG.info("----- started UserRankingWorker -----");
        try (Handle handle = dbi.open()) {
            updateUserRanking(handle);
        }
        LOG.info("----- finished UserRankingWorker -----");
    }

    private void updateUserRanking(Handle handle)
    {
        PaginatedUsers paginatedUsers = new PaginatedUsers(handle);
        List<User> users;
        while (!(users = paginatedUsers.nextUsers()).isEmpty()) {
            LOG.info("star: " + users.get(0).getStargazersCount()); // TODO: implement
        }
    }

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
            } else {
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
}

package com.github.k0kubun.github_ranking.worker;

import com.github.k0kubun.github_ranking.config.Config;
import com.github.k0kubun.github_ranking.model.User;
import com.github.k0kubun.github_ranking.model.UserRank;
import com.github.k0kubun.github_ranking.repository.PaginatedUsers;
import com.github.k0kubun.github_ranking.repository.dao.UserDao;
import com.github.k0kubun.github_ranking.repository.dao.UserRankDao;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserRankingWorker
        extends Worker
{
    private static final int ITERATE_MIN_STARS = 10;
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
    public void perform()
            throws Exception
    {
        while (userRankingQueue.poll(5, TimeUnit.SECONDS) == null) {
            if (isStopped) {
                return;
            }
        }
        LOG.info("----- started UserRankingWorker -----");
        try (Handle handle = dbi.open()) {
            UserRank lastRank = updateUpperRanking(handle);
            if (lastRank != null) {
                updateLowerRanking(handle, lastRank);
            }
        }
        LOG.info("----- finished UserRankingWorker -----");
    }

    private UserRank updateUpperRanking(Handle handle)
    {
        int count = handle.attach(UserDao.class).countUsers(); // warmup
        PaginatedUsers paginatedUsers = new PaginatedUsers(handle);
        List<User> users;

        List<UserRank> commitPendingRanks = new ArrayList<>(); // listed in stargazers_count DESC
        UserRank currentRank = null;
        int currentRankNum = 0;

        while (!(users = paginatedUsers.nextUsers()).isEmpty()) {
            // Shutdown immediately if requested, even if it's in progress.
            if (isStopped) {
                return null;
            }

            for (User user : users) {
                if (currentRank == null) {
                    currentRank = new UserRank(user.getStargazersCount(), 1);
                    currentRankNum = 1;
                }
                else if (currentRank.getStargazersCount() == user.getStargazersCount()) {
                    currentRankNum++;
                }
                else {
                    commitPendingRanks.add(currentRank);
                    currentRank = new UserRank(user.getStargazersCount(), currentRank.getRank() + currentRankNum);
                    currentRankNum = 1;
                }
            }

            if (!commitPendingRanks.isEmpty()) {
                commitRanks(handle, commitPendingRanks);
                commitPendingRanks.clear();
            }
            int rows = currentRank.getRank() + currentRankNum - 1;
            LOG.info("UserRankingWorker (" + calcProgress(rows, count) + ", " + Integer.valueOf(rows).toString() +
                    "/" + Integer.valueOf(count).toString() + " rows, rank " + Integer.valueOf(currentRank.getRank()).toString() + ", " +
                    Integer.valueOf(currentRank.getStargazersCount()).toString() + " stars)");

            // Switch the way to calculate ranking under 10 stars
            if (currentRank.getStargazersCount() <= ITERATE_MIN_STARS) {
                return currentRank;
            }
        }
        return currentRank;
    }

    private void updateLowerRanking(Handle handle, UserRank lastUserRank)
    {
        List<UserRank> userRanks = new ArrayList<>(); // listed in stargazers_count DESC
        userRanks.add(lastUserRank);

        int lastRank = lastUserRank.getRank();
        for (int lastStars = lastUserRank.getStargazersCount(); lastStars > 0; lastStars--) {
            LOG.info("UserRankingWorker for " + Integer.valueOf(lastStars - 1).toString());
            int count = handle.attach(UserDao.class).countUsersHavingStars(lastStars);
            userRanks.add(new UserRank(lastStars - 1, lastRank + count));
            lastRank += count;
        }
        commitRanks(handle, userRanks);
    }

    private void commitRanks(Handle handle, List<UserRank> userRanks)
    {
        // TODO: test this
        // `userRanks` is listed in stargazers_count DESC
        Integer minStars = lastOf(userRanks).getStargazersCount();
        //Integer highestRank = lastOf(userRanks).getRank();
        Integer maxStars = userRanks.get(0).getStargazersCount();
        //Integer lowestRank = userRanks.get(0).getRank();

        handle.useTransaction((conn, status) -> {
            conn.attach(UserRankDao.class).deleteStarsBetween(minStars, maxStars);
            //conn.attach(UserRankDao.class).deleteRankBetween(highestRank, lowestRank);
            conn.attach(UserRankDao.class).bulkInsert(userRanks);
        });
    }

    private UserRank lastOf(List<UserRank> userRanks)
    {
        return userRanks.get(userRanks.size() - 1);
    }

    private String calcProgress(int child, int parent)
    {
        return String.format("%.3f%%", (float) child / (float) parent);
    }
}

package com.github.k0kubun.github_ranking.worker;

import com.github.k0kubun.github_ranking.config.Config;
import com.github.k0kubun.github_ranking.github.GitHubClient;
import com.github.k0kubun.github_ranking.github.GitHubClientBuilder;
import com.github.k0kubun.github_ranking.model.User;
import com.github.k0kubun.github_ranking.repository.dao.LastUpdateDao;
import com.github.k0kubun.github_ranking.repository.dao.UserDao;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

// Scan all starred users
public class UserStarScanWorker extends UpdateUserWorker {
    private static final long TOKEN_USER_ID = 3138447; // k0kubun
    private static final long THRESHOLD_DAYS = 1; // At least later than Mar 6th
    private static final long MIN_RATE_LIMIT_REMAINING = 500; // Limit: 5000 / h
    private static final int BATCH_SIZE = 100;
    private static final Logger LOG = LoggerFactory.getLogger(UserStarScanWorker.class);

    private final BlockingQueue<Boolean> userStarScanQueue;
    private final DBI dbi;
    private final GitHubClientBuilder clientBuilder;
    private final Timestamp updateThreshold;

    public UserStarScanWorker(Config config) {
        super(config.getDatabaseConfig().getDataSource());
        userStarScanQueue = config.getQueueConfig().getUserFullScanQueue();
        clientBuilder = new GitHubClientBuilder(config.getDatabaseConfig().getDataSource());
        dbi = new DBI(config.getDatabaseConfig().getDataSource());
        updateThreshold = Timestamp.from(Instant.now().minus(THRESHOLD_DAYS, ChronoUnit.DAYS));
    }

    @Override
    public void perform() throws Exception {
        while (userStarScanQueue.poll(5, TimeUnit.SECONDS) == null) {
            if (isStopped) {
                return;
            }
        }

        GitHubClient client = clientBuilder.buildForUser(TOKEN_USER_ID);
        LOG.info(String.format("----- started UserStarScanWorker (API: %s/5000) -----", client.getRateLimitRemaining()));
        try (Handle handle = dbi.open()) {
            // 2 * (1000 / 30 min) ≒ 4000 / hour
            int numUsers = 1000;
            while (numUsers > 0) {
                // Check rate limit
                int remaining = client.getRateLimitRemaining();
                LOG.info(String.format("API remaining: %d/5000 (numUsers: %d)", remaining, numUsers));
                if (remaining < MIN_RATE_LIMIT_REMAINING) {
                    LOG.info(String.format("API remaining is smaller than %d. Stopping.", remaining));
                    break;
                }

                // Find a current cursor
                long lastUpdatedId = handle.attach(LastUpdateDao.class).userId(LastUpdateDao.USER_STAR_SCAN);
                long stars;
                if (lastUpdatedId == 0) {
                    stars = handle.attach(UserDao.class).maxStargazersCount();
                } else {
                    stars = handle.attach(UserDao.class).userStargazersCount(lastUpdatedId);
                }

                // Query a next batch
                List<User> users = new ArrayList<>();
                while (users.isEmpty()) {
                    users = handle.attach(UserDao.class).usersWithStarsAfter(stars, lastUpdatedId, Math.min(numUsers, BATCH_SIZE));
                    if (users.isEmpty()) {
                        stars = handle.attach(UserDao.class).nextStargazersCount(stars);
                        if (stars == 0) {
                            handle.attach(LastUpdateDao.class).resetUserId(LastUpdateDao.USER_STAR_SCAN);
                            LOG.info(String.format("--- completed and reset UserStarScanWorker (API: %s/5000) ---", client.getRateLimitRemaining()));
                            return;
                        }
                        lastUpdatedId = 0;
                    }
                }

                // Update users in the batch
                LOG.info(String.format("Batch size: %d (stars: %d)", users.size(), stars));
                lastUpdatedId = updateUsers(client, handle, users, lastUpdatedId);
                handle.attach(LastUpdateDao.class).updateUserId(LastUpdateDao.USER_FULL_SCAN, lastUpdatedId);

                numUsers -= users.size();
            }
        }
        LOG.info(String.format("----- finished UserStarScanWorker (API: %s/5000) -----", client.getRateLimitRemaining()));
    }

    private long updateUsers(GitHubClient client, Handle handle, List<User> users, long lastUpdatedId) throws IOException {
        for (User user : users) {
            Timestamp updatedAt = handle.attach(UserDao.class).userUpdatedAt(user.getId()); // TODO: Fix N+1
            if (updatedAt.before(updateThreshold)) {
                updateUser(handle, user, client);
                LOG.info(String.format("[%s] userId = %d (stars: %d)", user.getLogin(), user.getId(), user.getStargazersCount()));
            } else {
                LOG.info(String.format("Skip up-to-date user (id: %d, login: %s, updatedAt: %s)", user.getId(), user.getLogin(), updatedAt.toString()));
            }

            if (lastUpdatedId < user.getId()) {
                lastUpdatedId = user.getId();
            }
            if (isStopped) { // Shutdown immediately if requested
                break;
            }
        }
        return lastUpdatedId;
    }
}

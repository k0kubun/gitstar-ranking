package com.github.k0kubun.gitstar_ranking.worker;

import com.github.k0kubun.gitstar_ranking.config.Config;
import com.github.k0kubun.gitstar_ranking.github.GitHubClient;
import com.github.k0kubun.gitstar_ranking.github.GitHubClientBuilder;
import com.github.k0kubun.gitstar_ranking.model.User;
import com.github.k0kubun.gitstar_ranking.repository.dao.LastUpdateDao;
import com.github.k0kubun.gitstar_ranking.repository.dao.UserDao;
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
        userStarScanQueue = config.getQueueConfig().getUserStarScanQueue();
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
            int numUsers = 1000; // 2 * (1000 / 30 min) ≒ 4000 / hour
            int numChecks = 2000; // Avoid issuing too many queries by skips
            while (numUsers > 0 && numChecks > 0 && !isStopped) {
                // Find a current cursor
                long lastUpdatedId = handle.attach(LastUpdateDao.class).getCursor(LastUpdateDao.STAR_SCAN_USER_ID);
                long stars = handle.attach(LastUpdateDao.class).getCursor(LastUpdateDao.STAR_SCAN_STARS);
                if (stars == 0) {
                    stars = handle.attach(UserDao.class).maxStargazersCount();
                }

                // Query a next batch
                List<User> users = new ArrayList<>();
                while (users.isEmpty()) {
                    users = handle.attach(UserDao.class).usersWithStarsAfter(stars, lastUpdatedId, Math.min(numUsers, BATCH_SIZE));
                    if (users.isEmpty()) {
                        stars = handle.attach(UserDao.class).nextStargazersCount(stars);
                        if (stars == 0) {
                            handle.useTransaction((conn, status) -> {
                                conn.attach(LastUpdateDao.class).resetCursor(LastUpdateDao.STAR_SCAN_USER_ID);
                                conn.attach(LastUpdateDao.class).resetCursor(LastUpdateDao.STAR_SCAN_STARS);
                            });
                            LOG.info(String.format("--- completed and reset UserStarScanWorker (API: %s/5000) ---", client.getRateLimitRemaining()));
                            return;
                        }
                        lastUpdatedId = 0;
                    }
                }

                // Update users in the batch
                LOG.info(String.format("Batch size: %d (stars: %d)", users.size(), stars));
                for (User user : users) {
                    if (user.getLogin().equals("GITenberg") || user.getLogin().equals("gitpan")) {
                        LOG.info("Skipping a user with too many repositories: " + user.getLogin());
                        continue;
                    }

                    // Check rate limit
                    int remaining = client.getRateLimitRemaining();
                    LOG.info(String.format("API remaining: %d/5000 (numUsers: %d, numChecks: %d)", remaining, numUsers, numChecks));
                    if (remaining < MIN_RATE_LIMIT_REMAINING) {
                        LOG.info(String.format("API remaining is smaller than %d. Stopping.", remaining));
                        numChecks = 0;
                        break;
                    }

                    Timestamp updatedAt = handle.attach(UserDao.class).userUpdatedAt(user.getId()); // TODO: Fix N+1
                    if (updatedAt.before(updateThreshold)) {
                        updateUser(handle, user, client);
                        LOG.info(String.format("[%s] userId = %d (stars: %d)", user.getLogin(), user.getId(), user.getStargazersCount()));
                        numUsers--;
                    } else {
                        LOG.info(String.format("Skip up-to-date user (id: %d, login: %s, updatedAt: %s)", user.getId(), user.getLogin(), updatedAt.toString()));
                    }
                    numChecks--;

                    if (lastUpdatedId < user.getId()) {
                        lastUpdatedId = user.getId();
                    }
                    if (isStopped) { // Shutdown immediately if requested
                        break;
                    }
                }

                // Update the counter
                long nextUpdatedId = lastUpdatedId;
                long nextStars = stars;
                handle.useTransaction((conn, status) -> {
                    conn.attach(LastUpdateDao.class).updateCursor(LastUpdateDao.STAR_SCAN_USER_ID, nextUpdatedId);
                    conn.attach(LastUpdateDao.class).updateCursor(LastUpdateDao.STAR_SCAN_STARS, nextStars);
                });
            }
        }
        LOG.info(String.format("----- finished UserStarScanWorker (API: %s/5000) -----", client.getRateLimitRemaining()));
    }

    @Override
    public void updateUser(Handle handle, User user, GitHubClient client) throws IOException {
        super.updateUser(handle, user, client);
        try {
            Thread.sleep(500); // 0.5s: 1000 * 0.5s = 500s = 8.3 min (out of 15 min)
        } catch (InterruptedException e) {
            // suppress for override
        }
    }
}

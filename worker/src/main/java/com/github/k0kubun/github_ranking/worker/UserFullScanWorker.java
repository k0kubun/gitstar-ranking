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
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class UserFullScanWorker extends UpdateUserWorker {
    private static final long TOKEN_USER_ID = 3138447; // k0kubun
    private static final long THRESHOLD_DAYS = 14;
    private static final Logger LOG = LoggerFactory.getLogger(UserFullScanWorker.class);

    private final BlockingQueue<Boolean> userFullScanQueue;
    private final DBI dbi;
    private final GitHubClientBuilder clientBuilder;
    private final Timestamp updateThreshold;

    public UserFullScanWorker(Config config) {
        super(config.getDatabaseConfig().getDataSource());
        userFullScanQueue = config.getQueueConfig().getUserFullScanQueue();
        clientBuilder = new GitHubClientBuilder(config.getDatabaseConfig().getDataSource());
        dbi = new DBI(config.getDatabaseConfig().getDataSource());
        updateThreshold = Timestamp.from(Instant.now().minus(THRESHOLD_DAYS, ChronoUnit.DAYS));
    }

    @Override
    public void perform() throws Exception {
        while (userFullScanQueue.poll(5, TimeUnit.SECONDS) == null) {
            if (isStopped) {
                return;
            }
        }

        GitHubClient client = clientBuilder.buildForUser(TOKEN_USER_ID);
        LOG.info(String.format("----- started UserFullScanWorker (API: %s/5000) -----", client.getRateLimitRemaining()));
        try (Handle handle = dbi.open()) {
            long lastUserId = handle.attach(UserDao.class).lastId();

            // 1000 / 15 min ≒ 4000 / hour
            for (int i = 0; i < 10; i++) {
                long lastUpdatedId = handle.attach(LastUpdateDao.class).lastUserId();
                long nextUpdatedId = updateUsers(client, handle, lastUpdatedId);
                LOG.info(String.format("Last user_id: %d (%.3f%%)", nextUpdatedId, 100.0D * nextUpdatedId / lastUserId));

                if (nextUpdatedId <= lastUpdatedId) {
                    break;
                }
                handle.attach(LastUpdateDao.class).updateUserId(nextUpdatedId);
            }
        }
        LOG.info(String.format("----- finished UserFullScanWorker (API: %s/5000) -----", client.getRateLimitRemaining()));
    }

    private long updateUsers(GitHubClient client, Handle handle, long lastUpdatedId) throws IOException {
        List<User> users = client.getUsersSince(lastUpdatedId);
        if (users.isEmpty()) {
            return lastUpdatedId;
        }

        handle.attach(UserDao.class).bulkInsert(users);
        for (User user : users) {
            Timestamp updatedAt = handle.attach(UserDao.class).userUpdatedAt(user.getId()); // TODO: Fix N+1
            if (user.getId() <= 8464 /* bug fix */ || updatedAt.before(updateThreshold)) {
                updateUser(handle, user, client);
            } else {
                LOG.info(String.format("Skip up-to-date user (id: %d, login: %s, updatedAt: %s)", user.getId(), user.getLogin(), updatedAt.toString()));
            }
            if (lastUpdatedId < user.getId()) {
                lastUpdatedId = user.getId();
            }
        }
        return lastUpdatedId;
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

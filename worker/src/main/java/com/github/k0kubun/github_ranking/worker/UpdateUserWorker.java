package com.github.k0kubun.github_ranking.worker;

import com.github.k0kubun.github_ranking.config.Config;
import com.github.k0kubun.github_ranking.dao.AccessTokenDao;
import com.github.k0kubun.github_ranking.dao.UpdateUserJobDao;
import com.github.k0kubun.github_ranking.dao.UserDao;
import com.github.k0kubun.github_ranking.model.AccessToken;
import com.github.k0kubun.github_ranking.model.UpdateUserJob;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.kohsuke.github.GHUser;
import org.kohsuke.github.GitHub;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;

// This job must finish within TIMEOUT_MINUTES (1 min). Otherwise it will be infinitely retried.
public class UpdateUserWorker extends Worker
{
    private static final Integer TIMEOUT_MINUTES = 1;
    private static final Integer POLLING_INTERVAL_SECONDS = 1;
    private static final Logger LOG = Worker.buildLogger(UpdateUserWorker.class.getName());
    private final DBI dbi;

    public UpdateUserWorker(Config config)
    {
        super();
        dbi = new DBI(config.getDatabaseConfig().getDataSource());
    }

    // Dequeue a record from update_user_jobs and call updateUser().
    @Override
    public void perform() throws Exception
    {
        try (Handle handle = dbi.open()) {
            UpdateUserJobDao dao = handle.attach(UpdateUserJobDao.class);

            // Poll until it succeeds to acquire a job...
            Timestamp timeoutAt;
            while (dao.acquireUntil(timeoutAt = nextTimeout()) == 0) {
                TimeUnit.SECONDS.sleep(1);
            };

            // Succeeded to acquire a job. Fetch job to execute.
            UpdateUserJob job = dao.fetchByTimeout(timeoutAt);
            if (job == null) {
                throw new RuntimeException("Failed to fetch a job (timeoutAt = " + timeoutAt + ")");
            }

            // TODO: Log elapsed time
            LOG.info("started to updateUser: (userId = " + job.getUserId() + ")");
            updateUser(job.getUserId());
            LOG.info("finished to updateUser: (userId = " + job.getUserId() + ")");

            dao.delete(job.getId());
        }
    }

    private Timestamp nextTimeout()
    {
        return Timestamp.valueOf(LocalDateTime.now(ZoneId.of("UTC")).plusMinutes(TIMEOUT_MINUTES));
    }

    // Main part of this class. Given enqueued userId, it updates a user and his repositories.
    private void updateUser(Integer userId) throws IOException
    {
        importRepositories(userId);
        updateUserStars(userId);
    }

    // Just sync information of all repositories owned by specified user.
    // TODO: handle user deletion
    private void importRepositories(Integer userId) throws IOException
    {
        AccessToken token = dbi.onDemand(AccessTokenDao.class).find(1);
        GitHub github = GitHub.connectUsingOAuth(token.getToken());

        GHUser user = github.getUser("cookpad");
        LOG.info("Log: " + user.getLogin());
    }

    // From imported repositories, calculate the sum of stars and update user.
    private void updateUserStars(Integer userId)
    {
        // TODO: implement
    }
}

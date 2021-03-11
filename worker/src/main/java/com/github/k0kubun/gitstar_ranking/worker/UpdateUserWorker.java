package com.github.k0kubun.github_ranking.worker;

import com.github.k0kubun.github_ranking.github.GitHubClient;
import com.github.k0kubun.github_ranking.github.GitHubClientBuilder;
import com.github.k0kubun.github_ranking.model.Repository;
import com.github.k0kubun.github_ranking.model.UpdateUserJob;
import com.github.k0kubun.github_ranking.model.User;
import com.github.k0kubun.github_ranking.repository.DatabaseLock;
import com.github.k0kubun.github_ranking.repository.dao.RepositoryDao;
import com.github.k0kubun.github_ranking.repository.dao.UpdateUserJobDao;
import com.github.k0kubun.github_ranking.repository.dao.UserDao;
import io.sentry.Sentry;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpdateUserWorker
        extends Worker
{
    private static final Integer TIMEOUT_MINUTES = 1;
    private static final Logger LOG = LoggerFactory.getLogger(UpdateUserWorker.class);

    public final DBI dbi;
    public final GitHubClientBuilder clientBuilder;

    public UpdateUserWorker(DataSource dataSource)
    {
        super();
        clientBuilder = new GitHubClientBuilder(dataSource);
        dbi = new DBI(dataSource);
    }

    // Dequeue a record from update_user_jobs and call updateUser().
    @Override
    public void perform()
            throws Exception
    {
        try (Handle handle = dbi.open(); Handle lockHandle = dbi.open()) {
            DatabaseLock lock = new DatabaseLock(lockHandle);

            // Poll until it succeeds to acquire a job...
            Timestamp timeoutAt;
            while (acquireUntil(lock, handle, timeoutAt = nextTimeout()) == 0) {
                if (isStopped) {
                    return;
                }
                TimeUnit.SECONDS.sleep(1);
            }

            // Succeeded to acquire a job. Fetch job to execute.
            UpdateUserJobDao dao = handle.attach(UpdateUserJobDao.class);
            UpdateUserJob job = dao.fetchByTimeout(timeoutAt);
            if (job == null) {
                throw new RuntimeException("Failed to fetch a job (timeoutAt = " + timeoutAt + ")");
            }

            // TODO: Log elapsed time
            try {
                GitHubClient client = clientBuilder.buildForUser(job.getTokenUserId());
                final Long userId;
                if (job.getUserName() == null) {
                    userId = job.getUserId();
                } else {
                    userId = createUser(handle, job.getUserName(), client);
                }

                lock.withUserUpdate(userId, () -> {
                    User user = handle.attach(UserDao.class).find(userId);
                    LOG.info("UpdateUserWorker started: (userId = " + userId + ", login = " + user.getLogin() + ")");
                    updateUser(handle, user, client);
                    LOG.info("UpdateUserWorker finished: (userId = " + userId + ", login = " + user.getLogin() + ")");
                });
            }
            catch (Exception e) {
                Sentry.capture(e);
                LOG.error("Error in UpdateUserWorker! (userId = " + job.getUserId() + "): " + e.toString() + ": " + e.getMessage());
                // e.printStackTrace();
            }
            finally {
                dao.delete(job.getId());
            }
        }
    }

    // Create a pre-required user record for a give userName.
    private Long createUser(Handle handle, String userName, GitHubClient client) throws IOException {
        User user = client.getUserWithLogin(userName);
        List<User> users = new ArrayList<>();
        users.add(user);
        handle.attach(UserDao.class).bulkInsert(users);
        return user.getId();
    }

    // Main part of this class. Given enqueued userId, it updates a user and his repositories.
    // * Sync information of all repositories owned by specified user.
    // * Update fetched_at and updated_at, and set total stars to user.
    // TODO: Requeue if GitHub API limit exceeded
    public void updateUser(Handle handle, User user, GitHubClient client)
            throws IOException
    {
        Long userId = user.getId();
        String login = user.getLogin();

        try {
            LOG.debug("[" + login + "] finished: find User");

            if (!user.isOrganization()) {
                String newLogin = client.getLogin(userId);
                if (newLogin != null) {
                    handle.attach(UserDao.class).updateLogin(userId, newLogin);
                }
                LOG.debug("[" + login + "] finished: update Login");
            }

            List<Repository> repos = client.getPublicRepos(userId, user.isOrganization());
            LOG.debug("[" + login + "] finished: getPublicRepos");

            List<Long> repoIds = new ArrayList<>();
            for (Repository repo : repos) {
                repoIds.add(repo.getId());
            }

            handle.useTransaction((conn, status) -> {
                if (repoIds.size() > 0) {
                    conn.attach(RepositoryDao.class).deleteAllOwnedByExcept(userId, repoIds); // Delete obsolete ones
                } else {
                    conn.attach(RepositoryDao.class).deleteAllOwnedBy(userId);
                }
                conn.attach(RepositoryDao.class).bulkInsert(repos);
                LOG.debug("[" + login + "] finished: bulkInsert");
                conn.attach(UserDao.class).updateStars(userId, calcTotalStars(repos));
                LOG.debug("[" + login + "] finished: updateStars");
            });
            LOG.info("[" + login + "] imported repos: " + repos.size());
        }
        catch (GitHubClient.UserNotFoundException e) {
            LOG.error("UserNotFoundException error: " + e.getMessage());
            LOG.info("delete user: " + userId.toString());
            handle.useTransaction((conn, status) -> {
                conn.attach(UserDao.class).delete(userId);
                conn.attach(RepositoryDao.class).deleteAllOwnedBy(userId);
            });
        }
    }

    // Concurrently executing `dao.acquireUntil` causes deadlock. So this executes it in a lock.
    private long acquireUntil(DatabaseLock lock, Handle handle, Timestamp timeoutAt)
    {
        return lock.withUpdateUserJobs(() -> handle.attach(UpdateUserJobDao.class).acquireUntil(timeoutAt));
    }

    private Timestamp nextTimeout()
    {
        return Timestamp.valueOf(LocalDateTime.now(ZoneId.of("UTC")).plusMinutes(TIMEOUT_MINUTES));
    }

    private int calcTotalStars(List<Repository> repos)
    {
        int totalStars = 0;
        for (Repository repo : repos) {
            totalStars += repo.getStargazersCount();
        }
        return totalStars;
    }
}

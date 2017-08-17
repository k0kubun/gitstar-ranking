package com.github.k0kubun.github_ranking.worker;

import com.github.k0kubun.github_ranking.config.Config;
import com.github.k0kubun.github_ranking.dao.AccessTokenDao;
import com.github.k0kubun.github_ranking.dao.RepositoryDao;
import com.github.k0kubun.github_ranking.dao.UpdateUserJobDao;
import com.github.k0kubun.github_ranking.dao.UserDao;
import com.github.k0kubun.github_ranking.github.ClientBuilder;
import com.github.k0kubun.github_ranking.model.AccessToken;
import com.github.k0kubun.github_ranking.model.Repository;
import com.github.k0kubun.github_ranking.model.UpdateUserJob;
import com.github.k0kubun.github_ranking.model.User;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.RepositoryService;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;

// This job must finish within TIMEOUT_MINUTES (1 min). Otherwise it will be infinitely retried.
public class UpdateUserWorker extends Worker
{
    private static final Integer TIMEOUT_MINUTES = 1;
    private static final Integer POLLING_INTERVAL_SECONDS = 1;
    private static final String REDIS_USER_RANKING_KEY = "github-ranking:user:world:all";
    private static final String REDIS_ORG_RANKING_KEY = "github-ranking:organization:world:all";
    private static final String REDIS_REPO_RANKING_KEY = "github-ranking:repository:world:all";
    private static final Logger LOG = LoggerFactory.getLogger(UpdateUserWorker.class);

    private final DBI dbi;
    private final ClientBuilder clientBuilder;

    public UpdateUserWorker(DataSource dataSource)
    {
        super();
        clientBuilder = new ClientBuilder(dataSource);
        dbi = new DBI(dataSource);
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
                if (isStopped) {
                    return;
                }
                TimeUnit.SECONDS.sleep(1);
            };

            // Succeeded to acquire a job. Fetch job to execute.
            UpdateUserJob job = dao.fetchByTimeout(timeoutAt);
            if (job == null) {
                throw new RuntimeException("Failed to fetch a job (timeoutAt = " + timeoutAt + ")");
            }

            // TODO: Log elapsed time
            try {
                LOG.info("started to updateUser: (userId = " + job.getUserId() + ")");
                updateUser(handle, job.getUserId(), job.getTokenUserId());
                LOG.info("finished to updateUser: (userId = " + job.getUserId() + ")");
            } catch (Exception e) {
                LOG.error("Failed to updateUser! (userId = " + job.getUserId() + "): " + e.toString() + ": " + e.getMessage());
                e.printStackTrace();
            } finally {
                dao.delete(job.getId());
            }
        }
    }

    private Timestamp nextTimeout()
    {
        return Timestamp.valueOf(LocalDateTime.now(ZoneId.of("UTC")).plusMinutes(TIMEOUT_MINUTES));
    }

    // Main part of this class. Given enqueued userId, it updates a user and his repositories.
    // * Sync information of all repositories owned by specified user.
    // * Update fetched_at and updated_at, and set total stars to user.
    // TODO: Delete user if it's deleted on GitHub (was implemented in Sidekiq version)
    // TODO: Delete repos if they are deleted on GitHub (was implemented in Sidekiq version)
    // TODO: Requeue if GitHub API limit exceeded
    // TODO: Don't call API using "login" for key, and update users.login if updated
    private void updateUser(Handle handle, Integer userId, Integer tokenUserId) throws IOException
    {
        GitHubClient client = clientBuilder.buildForUser(tokenUserId);
        User user = handle.attach(UserDao.class).find(userId);

        List<Repository> repos = fetchPublicRepos(client, user.getLogin());
        handle.useTransaction((conn, status) -> {
            conn.attach(RepositoryDao.class).bulkInsert(repos);
            conn.attach(UserDao.class).updateStars(user.getId(), calcTotalStars(repos));
        });
        LOG.info("imported repos: " + repos.size());

        updateRankings(user, repos);
    }

    private List<Repository> fetchPublicRepos(GitHubClient client, String login) throws IOException
    {
        RepositoryService service = new RepositoryService(client);
        List<org.eclipse.egit.github.core.Repository> publicRepos = new ArrayList<>();

        for (org.eclipse.egit.github.core.Repository repo : service.getRepositories(login)) {
            if (!repo.isPrivate()) {
                publicRepos.add(repo);
            }
        }
        return Repository.fromGitHubRepos(publicRepos, login);
    }

    private int calcTotalStars(List<Repository> repos)
    {
        int totalStars = 0;
        for (Repository repo : repos) {
            totalStars += repo.getStargazersCount();
        }
        return totalStars;
    }

    // Update redis to have new stars.
    private void updateRankings(User user, List<Repository> repos)
    {
        Jedis jedis = new Jedis("localhost");

        int totalStars = calcTotalStars(repos);
        if (user.isOrganization()) {
            if (totalStars > 0) {
                jedis.zadd(REDIS_ORG_RANKING_KEY, totalStars, user.getId().toString());
            } else {
                jedis.zrem(REDIS_ORG_RANKING_KEY, user.getId().toString());
            }
        } else {
            if (totalStars > 0) {
                jedis.zadd(REDIS_USER_RANKING_KEY, totalStars, user.getId().toString());
            } else {
                jedis.zrem(REDIS_USER_RANKING_KEY, user.getId().toString());
            }
        }

        for (Repository repo : repos) {
            if (repo.getStargazersCount() > 0) {
                jedis.zadd(REDIS_REPO_RANKING_KEY, repo.getStargazersCount(), repo.getId().toString());
            } else {
                jedis.zrem(REDIS_REPO_RANKING_KEY, repo.getId().toString());
            }
        }
    }
}

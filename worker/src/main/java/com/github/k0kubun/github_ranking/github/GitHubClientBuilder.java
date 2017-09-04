package com.github.k0kubun.github_ranking.github;

import com.github.k0kubun.github_ranking.model.AccessToken;
import com.github.k0kubun.github_ranking.repository.dao.AccessTokenDao;
import com.github.k0kubun.github_ranking.worker.Worker;
import io.sentry.Sentry;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.skife.jdbi.v2.DBI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// This will have the logic to throttle GitHub API tokens.
public class GitHubClientBuilder
{
    private static final Logger LOG = LoggerFactory.getLogger(GitHubClientBuilder.class);
    private static final int RATE_LIMIT_ENABLED_THRESHOLD = 3000; // remaining over 60%

    private final DBI dbi;
    private List<AccessToken> tokens;

    public GitHubClientBuilder(DataSource dataSource)
    {
        dbi = new DBI(dataSource);
        tokens = new ArrayList<>();
    }

    public GitHubClient buildForUser(Integer userId)
    {
        AccessToken token = dbi.onDemand(AccessTokenDao.class).findByUserId(userId);
        return new GitHubClient(token.getToken());
    }

    public GitHubClient buildFromEnabled()
    {
        while (true) {
            AccessToken token = rotateToken();
            GitHubClient client = new GitHubClient(token.getToken());
            int remaining = client.getRateLimitRemaining();
            if (remaining == 0) {
                LOG.info("delete token: " + token.getToken() + " (" + Integer.valueOf(remaining).toString() + ")");
                tokens.remove(token);
            }
            else if (remaining > RATE_LIMIT_ENABLED_THRESHOLD) {
                LOG.info("found token: " + token.getToken() + " (" + Integer.valueOf(remaining).toString() + ")");
                return client;
            }
            else {
                LOG.info("failed token: " + token.getToken() + " (" + Integer.valueOf(remaining).toString() + ")");
                try {
                    TimeUnit.SECONDS.sleep(10);
                }
                catch (InterruptedException e) {
                    Sentry.capture(e);
                    LOG.info("interrupt");
                }
            }
        }
    }

    private AccessToken rotateToken()
    {
        if (tokens.isEmpty()) {
            tokens = dbi.onDemand(AccessTokenDao.class).allEnabledTokens();
        }
        return tokens.remove(0); // TODO: handle no tokens
    }
}

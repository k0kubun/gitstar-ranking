package com.github.k0kubun.github_ranking.github;

import com.github.k0kubun.github_ranking.model.AccessToken;
import com.github.k0kubun.github_ranking.repository.dao.AccessTokenDao;
import io.sentry.Sentry;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.skife.jdbi.v2.DBI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EnabledTokenFactory
        implements AccessTokenFactory
{
    private static final Logger LOG = LoggerFactory.getLogger(EnabledTokenFactory.class);
    private static final int RATE_LIMIT_ENABLED_THRESHOLD = 3000; // remaining over 60%

    private final DBI dbi;
    private List<AccessToken> tokens;

    public EnabledTokenFactory(DBI dbi)
    {
        this.dbi = dbi;
        tokens = new ArrayList<>();
    }

    @Override
    public String getToken()
    {
        while (true) {
            AccessToken token = rotateToken();
            GitHubClient client = new GitHubClient(token.getToken());
            int remaining = client.getRateLimitRemaining();

            // TODO: disable or delete if remaining is 0
            if (remaining > RATE_LIMIT_ENABLED_THRESHOLD) {
                LOG.info("found token: " + token.getToken() + " (" + Integer.valueOf(remaining).toString() + ")");
                return token.getToken();
            }
            else if (remaining == 0) {
                // just skip, maybe revoked or banned
                LOG.info("skipped token: " + token.getToken() + " (" + Integer.valueOf(remaining).toString() + ")");
            }
            else {
                LOG.info("failed token: " + token.getToken() + " (" + Integer.valueOf(remaining).toString() + ")");
                try {
                    TimeUnit.SECONDS.sleep(3);
                }
                catch (InterruptedException e) {
                    Sentry.capture(e);
                    LOG.info("interrupt in token factory");
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

package com.github.k0kubun.github_ranking.github;

import com.github.k0kubun.github_ranking.dao.repository.AccessTokenDao;
import com.github.k0kubun.github_ranking.model.AccessToken;

import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.skife.jdbi.v2.DBI;

// This will have the logic to throttle GitHub API tokens.
public class GitHubClientBuilder
{
    private final DBI dbi;

    public GitHubClientBuilder(DataSource dataSource)
    {
        dbi = new DBI(dataSource);
    }

    public GitHubClient buildForUser(Integer userId)
    {
        AccessToken token = dbi.onDemand(AccessTokenDao.class).findByUserId(userId);
        return new GitHubClient(token.getToken());
    }
}

package com.github.k0kubun.github_ranking.worker;

import com.github.k0kubun.github_ranking.config.Config;
import com.github.k0kubun.github_ranking.github.GitHubClient;
import com.github.k0kubun.github_ranking.github.GitHubClientBuilder;
import com.github.k0kubun.github_ranking.model.AccessToken;
import com.github.k0kubun.github_ranking.model.Repository;
import com.github.k0kubun.github_ranking.model.UpdateUserJob;
import com.github.k0kubun.github_ranking.model.User;
import com.github.k0kubun.github_ranking.repository.DatabaseLock;
import com.github.k0kubun.github_ranking.repository.dao.AccessTokenDao;
import com.github.k0kubun.github_ranking.repository.dao.RepositoryDao;
import com.github.k0kubun.github_ranking.repository.dao.UpdateUserJobDao;
import com.github.k0kubun.github_ranking.repository.dao.UserDao;

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

// This job must finish within TIMEOUT_MINUTES (1 min). Otherwise it will be infinitely retried.
public class UpdateStarredUserWorker
        extends UpdateUserWorker
{
    public UpdateStarredUserWorker(DataSource dataSource)
    {
        super(dataSource);
    }
}

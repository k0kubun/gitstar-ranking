package com.github.k0kubun.github_ranking.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import javax.sql.DataSource;

// Its queue may be configurable in the future. So it's placed here but currently just a group of queues.
public class QueueConfig
{
    private final BlockingQueue<Void> userRankingQueue;
    private final BlockingQueue<Void> orgRankingQueue;
    private final BlockingQueue<Void> repoRankingQueue;

    public QueueConfig()
    {
        userRankingQueue = new LinkedBlockingQueue<>();
        orgRankingQueue = new LinkedBlockingQueue<>();
        repoRankingQueue = new LinkedBlockingQueue<>();
    }

    public BlockingQueue<Void> getUserRankingQueue()
    {
        return userRankingQueue;
    }

    public BlockingQueue<Void> getOrgRankingQueue()
    {
        return orgRankingQueue;
    }

    public BlockingQueue<Void> getRepoRankingQueue()
    {
        return repoRankingQueue;
    }
}

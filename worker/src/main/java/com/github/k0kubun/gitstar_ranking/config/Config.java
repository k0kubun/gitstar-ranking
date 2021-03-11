package com.github.k0kubun.gitstar_ranking.config;

import com.github.k0kubun.gitstar_ranking.config.QueueConfig;

import java.util.Map;

public class Config
{
    private final Map<String, String> env;
    private final DatabaseConfig databaseConfig;
    private final QueueConfig queueConfig;

    public Config(Map<String, String> env)
    {
        this.env = env;
        this.databaseConfig = new DatabaseConfig(env);
        this.queueConfig = new QueueConfig();
    }

    public DatabaseConfig getDatabaseConfig()
    {
        return databaseConfig;
    }

    public QueueConfig getQueueConfig()
    {
        return queueConfig;
    }

    public String getServerAddr()
    {
        return "127.0.0.1";
    }

    public Integer getServerPort()
    {
        return 5001;
    }
}

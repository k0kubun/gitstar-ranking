package com.github.k0kubun.github_ranking.config;

import java.util.Map;

public class Config
{
    private final Map<String, String> env;
    private final DatabaseConfig databaseConfig;

    public Config(Map<String, String> env)
    {
        this.env = env;
        this.databaseConfig = new DatabaseConfig(env);
    }

    public DatabaseConfig getDatabaseConfig()
    {
        return databaseConfig;
    }
}

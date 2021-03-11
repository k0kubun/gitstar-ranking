package com.github.k0kubun.gitstar_ranking.config;

import org.postgresql.ds.PGSimpleDataSource;

import java.util.Map;

import javax.sql.DataSource;

public class DatabaseConfig
{
    private static final String DEFAULT_HOST = "127.0.0.1";
    private static final Integer DEFAULT_PORT = 5432;
    private static final String DEFAULT_USER = "postgres";
    private static final String DEFAULT_PASSWORD = "";
    private static final String DEFAULT_DATABASE = "gitstar_ranking";

    private final Map<String, String> env;

    public DatabaseConfig(Map<String, String> env)
    {
        this.env = env;
    }

    public DataSource getDataSource()
    {
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setUrl(getUrl());
        dataSource.setUser(getUser());
        dataSource.setPassword(getPassword());
        return dataSource;
    }

    public String getUrl()
    {
        return String.format("jdbc:postgresql://%s:%d/%s",
                getHost(),
                getPort(),
                getDatabaseName());
    }

    public String getUser()
    {
        return env.getOrDefault("DATABASE_USER", DEFAULT_USER);
    }

    public String getPassword()
    {
        return env.getOrDefault("DATABASE_PASSWORD", DEFAULT_PASSWORD);
    }

    private String getHost()
    {
        return env.getOrDefault("DATABASE_HOST", DEFAULT_HOST);
    }

    private Integer getPort()
    {
        return getIntegerOrDefault("DATABASE_PORT", DEFAULT_PORT);
    }

    private String getDatabaseName()
    {
        return env.getOrDefault("DATABASE_NAME", DEFAULT_DATABASE);
    }

    private Integer getIntegerOrDefault(String key, Integer defaultValue)
    {
        if (env.containsKey(key)) {
            String value = env.get(key);
            return Integer.parseInt(value);
        }
        else {
            return defaultValue;
        }
    }
}

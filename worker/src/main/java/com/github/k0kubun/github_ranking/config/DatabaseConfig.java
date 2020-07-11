package com.github.k0kubun.github_ranking.config;

import com.mysql.cj.jdbc.MysqlDataSource;

import java.util.Map;

import javax.sql.DataSource;

public class DatabaseConfig
{
    private static final String DEFAULT_HOST = "127.0.0.1";
    private static final Integer DEFAULT_PORT = 3306;
    private static final String DEFAULT_USER = "root";
    private static final String DEFAULT_PASSWORD = "";
    private static final String DEFAULT_DATABASE = "github_ranking";

    private final Map<String, String> env;

    public DatabaseConfig(Map<String, String> env)
    {
        this.env = env;
    }

    public DataSource getDataSource()
    {
        MysqlDataSource mysqlDataSource = new MysqlDataSource();
        mysqlDataSource.setUrl(getUrl());
        mysqlDataSource.setUser(getUser());
        mysqlDataSource.setPassword(getPassword());
        return mysqlDataSource;
    }

    public String getUrl()
    {
        return String.format("jdbc:mysql://%s:%d/%s",
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

package com.github.k0kubun.github_ranking.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

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
        return new HikariDataSource(getHikariConfig());
    }

    public String getUrl()
    {
        return String.format("jdbc:mysql://%s:%d/%s?useUnicode=true&characterEncoding=utf8mb4",
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

    // HikariCP's default = 10: https://github.com/brettwooldridge/HikariCP#frequently-used
    private Integer getMaxPoolSize()
    {
        return getIntegerOrDefault("DATABASE_MAX_POOL_SIZE", 10);
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

    private HikariConfig getHikariConfig()
    {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(getUrl());
        config.setUsername(getUser());
        config.setPassword(getPassword());
        config.setMaximumPoolSize(getMaxPoolSize());

        // Followings are recommendation from HikariCP https://github.com/brettwooldridge/HikariCP#initialization
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        return config;
    }
}

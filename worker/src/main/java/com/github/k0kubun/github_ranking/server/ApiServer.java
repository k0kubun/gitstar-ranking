package com.github.k0kubun.github_ranking.server;

import com.github.k0kubun.github_ranking.config.Config;
import com.github.k0kubun.github_ranking.config.QueueConfig;
import com.treasuredata.underwrap.UnderwrapServer;

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

public class ApiServer
{
    private final Config config;
    private final UnderwrapServer server;

    public ApiServer(Class<? extends UnderwrapServer.UnderwrapApplication> applicationClass, Config config)
    {
        this.config = config;
        server = new UnderwrapServer(applicationClass);
    }

    public void start()
    {
        server.start(
                buildContextMap(),
                di -> di.setDeploymentName("github-ranking-api-server"),
                serverBuilder -> serverBuilder.addHttpListener(config.getServerPort(), config.getServerAddr())
        );
    }

    public void stop()
    {
        server.stop();
    }

    // This will be available via @Context annotation.
    private Map<Class<?>, Object> buildContextMap()
    {
        Map<Class<?>, Object> contextMap = new HashMap<>();
        contextMap.put(QueueConfig.class, config.getQueueConfig());
        return contextMap;
    }
}

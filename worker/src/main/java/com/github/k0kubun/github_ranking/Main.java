package com.github.k0kubun.github_ranking;

import com.github.k0kubun.github_ranking.config.Config;
import com.github.k0kubun.github_ranking.worker.UpdateUserWorker;
import com.github.k0kubun.github_ranking.worker.WorkerManager;
import javax.sql.DataSource;

public class Main
{
    private static final int NUM_UPDATE_USER_WORKERS = 3;

    public static void main(String[] args)
    {
        Config config = new Config(System.getenv());

        WorkerManager workers = buildWorkers(config);
        workers.start();
        Runtime.getRuntime().addShutdownHook(new Thread(workers::stop));
    }

    private static WorkerManager buildWorkers(Config config)
    {
        DataSource dataSource = config.getDatabaseConfig().getDataSource();

        WorkerManager workers = new WorkerManager();
        for (int i = 0; i < NUM_UPDATE_USER_WORKERS; i++) {
            workers.add(new UpdateUserWorker(dataSource));
        }
        return workers;
    }
}

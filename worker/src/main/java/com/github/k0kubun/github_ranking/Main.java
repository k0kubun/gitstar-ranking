package com.github.k0kubun.github_ranking;

import com.github.k0kubun.github_ranking.config.Config;
import com.github.k0kubun.github_ranking.worker.UpdateUserWorker;
import com.github.k0kubun.github_ranking.worker.WorkerManager;
import javax.sql.DataSource;

public class Main
{
    public static void main(String[] args)
    {
        Config config = new Config(System.getenv());
        WorkerManager workers = buildWorkers(config);

        Runtime.getRuntime().addShutdownHook(new Thread(workers::stop));
        workers.start();
    }

    private static WorkerManager buildWorkers(Config config)
    {
        DataSource dataSource = config.getDatabaseConfig().getDataSource();

        WorkerManager workers = new WorkerManager();
        workers.add(new UpdateUserWorker(dataSource));
        return workers;
    }
}

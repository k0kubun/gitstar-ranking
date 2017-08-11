package com.github.k0kubun.github_ranking;

import com.github.k0kubun.github_ranking.config.Config;
import com.github.k0kubun.github_ranking.worker.UpdateUserWorker;
import com.github.k0kubun.github_ranking.worker.WorkerManager;

public class Main
{
    public static void main(String[] args)
    {
        Config config = new Config(System.getenv());
        WorkerManager workers = buildWorkers(config);

        workers.start();
        Runtime.getRuntime().addShutdownHook(new Thread(workers::stop));
    }

    private static WorkerManager buildWorkers(Config config)
    {
        WorkerManager workers = new WorkerManager();
        workers.add(new UpdateUserWorker(config));
        return workers;
    }
}

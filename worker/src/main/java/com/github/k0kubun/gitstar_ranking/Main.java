package com.github.k0kubun.github_ranking;

import com.github.k0kubun.github_ranking.config.Config;
import com.github.k0kubun.github_ranking.worker.*;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.sentry.Sentry;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main
{
    private static final Logger LOG = LoggerFactory.getLogger(Main.class);
    private static final int NUM_UPDATE_USER_WORKERS = 2;

    private static final Config config = new Config(System.getenv());

    public static void main(String[] args)
    {
        Sentry.init(System.getenv().get("SENTRY_DSN"));

        ScheduledExecutorService scheduler = buildAndRunScheduler();

        WorkerManager workers = buildWorkers(config);
        workers.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            shutdownAndAwaitTermination(scheduler);
            workers.stop();
        }));
    }

    private static ScheduledExecutorService buildAndRunScheduler()
    {
        ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setNameFormat("scheduler-%d")
                .setUncaughtExceptionHandler((t, e) -> {
                    Sentry.capture(e);
                    LOG.error("Uncaught exception at scheduler: " + e.getMessage());
                })
                .build();
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(threadFactory);

        // Schedule at most every 8 hours
        scheduler.scheduleWithFixedDelay(() -> scheduleIfEmpty(config.getQueueConfig().getUserRankingQueue()), 1, 8, TimeUnit.HOURS);
        // scheduler.scheduleWithFixedDelay(() -> { scheduleIfEmpty(config.getQueueConfig().getRepoRankingQueue()); }, 5, 8, TimeUnit.HOURS);
        // scheduler.scheduleWithFixedDelay(() -> { scheduleIfEmpty(config.getQueueConfig().getOrgRankingQueue()); }, 7, 8, TimeUnit.HOURS);

        // Schedule at most every 30 minutes
        scheduler.scheduleWithFixedDelay(() -> scheduleIfEmpty(config.getQueueConfig().getUserStarScanQueue()), 0, 30, TimeUnit.MINUTES);
        scheduler.scheduleWithFixedDelay(() -> scheduleIfEmpty(config.getQueueConfig().getUserFullScanQueue()), 15, 30, TimeUnit.MINUTES);

        return scheduler;
    }

    private static void scheduleIfEmpty(BlockingQueue<Boolean> queue)
    {
        if (queue.size() == 0) {
            try {
                queue.put(true);
            }
            catch (InterruptedException e) {
                Sentry.capture(e);
                LOG.error("Scheduling interrupted: " + e.getMessage());
            }
        }
    }

    private static WorkerManager buildWorkers(Config config)
    {
        DataSource dataSource = config.getDatabaseConfig().getDataSource();

        WorkerManager workers = new WorkerManager();
        for (int i = 0; i < NUM_UPDATE_USER_WORKERS; i++) {
            workers.add(new UpdateUserWorker(dataSource));
        }
        workers.add(new UserRankingWorker(config));
        workers.add(new UserStarScanWorker(config));
        workers.add(new UserFullScanWorker(config));
        return workers;
    }

    // https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/ExecutorService.html
    private static void shutdownAndAwaitTermination(ExecutorService executor)
    {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow();
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    LOG.error("Failed to shutdown scheduler");
                }
            }
        }
        catch (InterruptedException e) {
            Sentry.capture(e);
            LOG.error("Scheduler shutdown interrupted: " + e.getMessage());
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}

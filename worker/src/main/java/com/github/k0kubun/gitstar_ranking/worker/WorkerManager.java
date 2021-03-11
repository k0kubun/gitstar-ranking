package com.github.k0kubun.github_ranking.worker;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.sentry.Sentry;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WorkerManager
{
    private static final Logger LOG = LoggerFactory.getLogger(WorkerManager.class);
    private final List<Worker> workers;
    private List<Future<Void>> futures;

    public WorkerManager()
    {
        workers = new ArrayList<>();
        futures = new ArrayList<>();
    }

    public void add(Worker worker)
    {
        workers.add(worker);
    }

    public void start()
    {
        ExecutorService executor = Executors.newFixedThreadPool(workers.size(),
                new ThreadFactoryBuilder()
                        .setNameFormat("github-ranking-worker-%d")
                        .setUncaughtExceptionHandler((t, e) -> {
                            Sentry.capture(e);
                            LOG.error("Uncaught exception at worker: " + e.getMessage());
                        }).build());

        LOG.info("Starting workers...");
        for (Worker worker : workers) {
            futures.add(executor.submit(worker));
        }
    }

    public void stop()
    {
        LOG.info("Shutting down workers...");
        for (Worker worker : workers) {
            worker.stop();
        }

        LOG.info("Stopping workers...");
        int i = 0;
        for (Future<Void> future : futures) {
            try {
                future.get(60, TimeUnit.SECONDS);
                i++;
                LOG.info("Stopped worker (" + i + "/" + futures.size() + ")");
            }
            catch (TimeoutException e) {
                Sentry.capture(e);
                LOG.error("Timed out to stop worker!: " + e.getMessage());
            }
            catch (InterruptedException e) {
                Sentry.capture(e);
                LOG.error("Interrupted on stopping worker!: " + e.getMessage());
            }
            catch (ExecutionException e) {
                Sentry.capture(e);
                LOG.error("Execution failed on stopping worker!: " + e.getMessage());
            }
        }
        futures.clear();
    }
}

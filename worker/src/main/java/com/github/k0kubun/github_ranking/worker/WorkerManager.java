package com.github.k0kubun.github_ranking.worker;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

public class WorkerManager
{
    private static final Logger LOG = Worker.buildLogger("worker_manager.log", WorkerManager.class.getName());
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
                            LOG.severe("Uncaught exception: " + e.getMessage());
                        }).build());
        for (Worker worker : workers) {
            futures.add(executor.submit(worker));
        }
    }

    public void stop()
    {
        for (Worker worker : workers) {
            worker.stop();
        }
        for (Future<Void> future : futures) {
            try {
                future.get(60, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                LOG.severe("Timed out to stop worker!: " + e.getMessage());
            } catch (InterruptedException e) {
                LOG.severe("Interrupted on stopping worker!: " + e.getMessage());
            } catch (ExecutionException e) {
                LOG.severe("Execution failed on stopping worker!: " + e.getMessage());
            }
        }
        futures.clear();
    }
}

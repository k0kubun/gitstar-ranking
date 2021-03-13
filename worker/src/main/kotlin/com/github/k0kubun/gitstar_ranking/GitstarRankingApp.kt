package com.github.k0kubun.gitstar_ranking

import com.github.k0kubun.gitstar_ranking.config.Config
import com.github.k0kubun.gitstar_ranking.worker.UpdateUserWorker
import com.github.k0kubun.gitstar_ranking.worker.UserFullScanWorker
import com.github.k0kubun.gitstar_ranking.worker.UserRankingWorker
import com.github.k0kubun.gitstar_ranking.worker.UserStarScanWorker
import com.github.k0kubun.gitstar_ranking.worker.WorkerManager
import com.google.common.util.concurrent.ThreadFactoryBuilder
import io.sentry.Sentry
import java.lang.InterruptedException
import java.lang.Thread
import java.util.concurrent.BlockingQueue
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import org.slf4j.LoggerFactory

private const val NUM_UPDATE_USER_WORKERS = 2

class GitstarRankingApp {
    private val logger = LoggerFactory.getLogger(GitstarRankingApp::class.java)
    private val config = Config(System.getenv())

    fun run() {
        val scheduler = buildAndRunScheduler()
        val workers = buildWorkers(config)
        workers.start()
        Runtime.getRuntime().addShutdownHook(Thread {
            shutdownAndAwaitTermination(scheduler)
            workers.stop()
        })
    }

    private fun buildAndRunScheduler(): ScheduledExecutorService {
        val threadFactory = ThreadFactoryBuilder()
            .setNameFormat("scheduler-%d")
            .setUncaughtExceptionHandler { _: Thread?, e: Throwable ->
                Sentry.capture(e)
                logger.error("Uncaught exception at scheduler: " + e.message)
            }
            .build()
        val scheduler = Executors.newSingleThreadScheduledExecutor(threadFactory)

        // Schedule at most every 8 hours
        scheduler.scheduleWithFixedDelay({ scheduleIfEmpty(config.queueConfig.userRankingQueue) }, 1, 8, TimeUnit.HOURS)
        // scheduler.scheduleWithFixedDelay(() -> { scheduleIfEmpty(config.getQueueConfig().getRepoRankingQueue()); }, 5, 8, TimeUnit.HOURS);
        // scheduler.scheduleWithFixedDelay(() -> { scheduleIfEmpty(config.getQueueConfig().getOrgRankingQueue()); }, 7, 8, TimeUnit.HOURS);

        // Schedule at most every 30 minutes
        scheduler.scheduleWithFixedDelay({ scheduleIfEmpty(config.queueConfig.userStarScanQueue) }, 0, 30, TimeUnit.MINUTES)
        scheduler.scheduleWithFixedDelay({ scheduleIfEmpty(config.queueConfig.userFullScanQueue) }, 15, 30, TimeUnit.MINUTES)
        return scheduler
    }

    private fun scheduleIfEmpty(queue: BlockingQueue<Boolean>) {
        if (queue.size == 0) {
            try {
                queue.put(true)
            } catch (e: InterruptedException) {
                Sentry.capture(e)
                logger.error("Scheduling interrupted: " + e.message)
            }
        }
    }

    private fun buildWorkers(config: Config): WorkerManager {
        val dataSource = config.databaseConfig.dataSource
        val workers = WorkerManager()
        repeat(NUM_UPDATE_USER_WORKERS) {
            workers.add(UpdateUserWorker(dataSource))
        }
        workers.add(UserRankingWorker(config))
        workers.add(UserStarScanWorker(config))
        workers.add(UserFullScanWorker(config))
        return workers
    }

    // https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/ExecutorService.html
    private fun shutdownAndAwaitTermination(executor: ExecutorService) {
        executor.shutdown()
        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow()
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    logger.error("Failed to shutdown scheduler")
                }
            }
        } catch (e: InterruptedException) {
            Sentry.capture(e)
            logger.error("Scheduler shutdown interrupted: " + e.message)
            executor.shutdownNow()
            Thread.currentThread().interrupt()
        }
    }
}

fun main() {
    GitstarRankingApp().run()
}

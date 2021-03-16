package com.github.k0kubun.gitstar_ranking

import com.github.k0kubun.gitstar_ranking.workers.RankingWorker
import com.github.k0kubun.gitstar_ranking.workers.UpdateUserWorker
import com.github.k0kubun.gitstar_ranking.workers.UserFullScanWorker
import com.github.k0kubun.gitstar_ranking.workers.UserStarScanWorker
import com.github.k0kubun.gitstar_ranking.workers.WorkerManager
import com.google.common.util.concurrent.ThreadFactoryBuilder
import io.sentry.Sentry
import java.lang.InterruptedException
import java.lang.RuntimeException
import java.lang.Thread
import java.util.concurrent.BlockingQueue
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import org.slf4j.LoggerFactory

private const val NUM_UPDATE_USER_WORKERS = 2

class GitstarRankingApp {
    private val logger = LoggerFactory.getLogger(GitstarRankingApp::class.simpleName)
    private val config = GitstarRankingConfiguration()

    fun run(schedule: Boolean) {
        val scheduler = buildAndRunScheduler(schedule)
        val workers = buildWorkers(config)
        workers.start()
        Runtime.getRuntime().addShutdownHook(Thread {
            shutdownAndAwaitTermination(scheduler)
            workers.stop()
        })
    }

    private fun buildAndRunScheduler(schedule: Boolean): ScheduledExecutorService {
        val threadFactory = ThreadFactoryBuilder()
            .setNameFormat("scheduler-%d")
            .setUncaughtExceptionHandler { _: Thread?, e: Throwable ->
                Sentry.captureException(e)
                logger.error("Uncaught exception at scheduler: " + e.message)
            }
            .build()
        val scheduler = Executors.newSingleThreadScheduledExecutor(threadFactory)
        if (!schedule) return scheduler

        // Schedule at most every 8 hours
        scheduler.scheduleWithFixedDelay({ scheduleIfEmpty(config.queue.rankingQueue) }, 0, 8, TimeUnit.HOURS)

        // Schedule at most every 30 minutes
        //scheduler.scheduleWithFixedDelay({ scheduleIfEmpty(config.queue.userStarScanQueue) }, 0, 30, TimeUnit.MINUTES)
        //scheduler.scheduleWithFixedDelay({ scheduleIfEmpty(config.queue.userFullScanQueue) }, 15, 30, TimeUnit.MINUTES)
        return scheduler
    }

    private fun scheduleIfEmpty(queue: BlockingQueue<Boolean>) {
        if (queue.size == 0) {
            queue.put(true)
        }
    }

    private fun buildWorkers(config: GitstarRankingConfiguration): WorkerManager {
        val workers = WorkerManager()
        repeat(NUM_UPDATE_USER_WORKERS) {
            workers.add(UpdateUserWorker(config.database.dslContext))
        }
        workers.add(UserStarScanWorker(config))
        workers.add(UserFullScanWorker(config))
        workers.add(RankingWorker(config))
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
            Sentry.captureException(e)
            logger.error("Scheduler shutdown interrupted: " + e.message)
            executor.shutdownNow()
            Thread.currentThread().interrupt()
        }
    }
}

fun main(args: Array<String>) {
    System.getenv("SENTRY_DSN")?.let { dsn ->
        Sentry.init { options ->
            options.dsn = dsn
        }
    }

    var schedule = true
    args.forEach {
        when (it) {
            "--no-schedule" -> schedule = false
            else -> throw RuntimeException("Unexpected argument '$it'")
        }
    }
    GitstarRankingApp().run(schedule)
}

package com.github.k0kubun.gitstar_ranking.workers

import com.google.common.util.concurrent.ThreadFactoryBuilder
import io.sentry.Sentry
import java.lang.InterruptedException
import java.lang.Thread
import java.lang.Void
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import org.slf4j.LoggerFactory

class WorkerManager {
    private val logger = LoggerFactory.getLogger(WorkerManager::class.simpleName)
    private val workers: MutableList<Worker> = mutableListOf()
    private val futures: MutableList<Future<Void?>> = mutableListOf()

    fun add(worker: Worker) {
        workers.add(worker)
    }

    fun start() {
        val executor = Executors.newFixedThreadPool(workers.size,
            ThreadFactoryBuilder()
                .setNameFormat("worker-%d")
                .setUncaughtExceptionHandler { _: Thread?, e: Throwable ->
                    Sentry.captureException(e)
                    logger.error("Uncaught exception at worker: " + e.message)
                }.build())
        logger.info("Starting workers...")
        for (worker in workers) {
            futures.add(executor.submit(worker))
        }
    }

    fun stop() {
        logger.info("Shutting down workers...")
        workers.forEach { worker ->
            worker.stop()
        }
        logger.info("Stopping workers...")
        var i = 0
        futures.forEach { future ->
            try {
                future[60, TimeUnit.SECONDS]
                i++
                logger.info("Stopped worker ($i/${futures.size})")
            } catch (e: TimeoutException) {
                Sentry.captureException(e)
                logger.error("Timed out to stop worker!: ${e.message}")
            } catch (e: InterruptedException) {
                Sentry.captureException(e)
                logger.error("Interrupted on stopping worker!: ${e.message}")
            } catch (e: ExecutionException) {
                Sentry.captureException(e)
                logger.error("Execution failed on stopping worker!: ${e.message}")
            }
        }
        futures.clear()
    }
}

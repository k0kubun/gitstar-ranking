package com.github.k0kubun.gitstar_ranking.worker

import java.lang.Void
import java.util.concurrent.Executors
import com.google.common.util.concurrent.ThreadFactoryBuilder
import java.lang.Thread
import io.sentry.Sentry
import java.util.concurrent.TimeUnit
import java.lang.InterruptedException
import java.util.ArrayList
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future
import java.util.concurrent.TimeoutException
import org.slf4j.LoggerFactory

class WorkerManager {
    private val workers: MutableList<Worker>
    private val futures: MutableList<Future<Void?>>
    fun add(worker: Worker) {
        workers.add(worker)
    }

    fun start() {
        val executor = Executors.newFixedThreadPool(workers.size,
            ThreadFactoryBuilder()
                .setNameFormat("github-ranking-worker-%d")
                .setUncaughtExceptionHandler { _: Thread?, e: Throwable ->
                    Sentry.capture(e)
                    LOG.error("Uncaught exception at worker: " + e.message)
                }.build())
        LOG.info("Starting workers...")
        for (worker in workers) {
            futures.add(executor.submit(worker))
        }
    }

    fun stop() {
        LOG.info("Shutting down workers...")
        for (worker in workers) {
            worker.stop()
        }
        LOG.info("Stopping workers...")
        var i = 0
        for (future in futures) {
            try {
                future[60, TimeUnit.SECONDS]
                i++
                LOG.info("Stopped worker (" + i + "/" + futures.size + ")")
            } catch (e: TimeoutException) {
                Sentry.capture(e)
                LOG.error("Timed out to stop worker!: " + e.message)
            } catch (e: InterruptedException) {
                Sentry.capture(e)
                LOG.error("Interrupted on stopping worker!: " + e.message)
            } catch (e: ExecutionException) {
                Sentry.capture(e)
                LOG.error("Execution failed on stopping worker!: " + e.message)
            }
        }
        futures.clear()
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(WorkerManager::class.java)
    }

    init {
        workers = ArrayList()
        futures = ArrayList()
    }
}

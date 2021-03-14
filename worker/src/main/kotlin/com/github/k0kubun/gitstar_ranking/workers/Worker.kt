package com.github.k0kubun.gitstar_ranking.workers

import io.sentry.Sentry
import java.lang.Exception
import java.lang.UnsupportedOperationException
import java.lang.Void
import java.util.concurrent.Callable
import org.slf4j.LoggerFactory

abstract class Worker : Callable<Void?> {
    private val logger = LoggerFactory.getLogger(Worker::class.simpleName)

    // Subclass must prepare to shut down if this is true.
    var isStopped = false
    override fun call(): Void? {
        while (!isStopped) {
            try {
                perform()
            } catch (e: Exception) {
                handleException(e)
            }
        }
        return null
    }

    // Request to stop thread without blocking.
    fun stop() {
        isStopped = true
    }

    // Perform one iteration of worker loop. This must be overridden in subclass.
    open fun perform() {
        throw UnsupportedOperationException("run() method was not overridden")
    }

    private fun handleException(e: Exception) {
        Sentry.captureException(e)
        logger.error("Unhandled exception!: ${e.stackTraceToString()}")
    }
}

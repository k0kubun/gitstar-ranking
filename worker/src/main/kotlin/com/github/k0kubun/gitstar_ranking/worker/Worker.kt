package com.github.k0kubun.gitstar_ranking.worker

import java.util.concurrent.Callable
import java.lang.Void
import java.lang.Exception
import kotlin.Throws
import java.lang.UnsupportedOperationException
import io.sentry.Sentry
import org.slf4j.LoggerFactory

abstract class Worker : Callable<Void?> {
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
    @Throws(Exception::class)
    open fun perform() {
        throw UnsupportedOperationException("run() method was not overridden")
    }

    private fun handleException(e: Exception) {
        Sentry.capture(e)
        LOG.error("Unhandled exception!: " + e.message)
        // e.printStackTrace();
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(Worker::class.java)
    }
}

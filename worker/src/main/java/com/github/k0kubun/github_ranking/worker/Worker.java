package com.github.k0kubun.github_ranking.worker;

import io.sentry.Sentry;

import java.io.IOException;
import java.lang.UnsupportedOperationException;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Worker
        implements Callable<Void>
{
    private static final Logger LOG = LoggerFactory.getLogger(Worker.class);

    // Subclass must prepare to shut down if this is true.
    public boolean isStopped;

    public Worker()
    {
        this.isStopped = false;
    }

    @Override
    public Void call()
    {
        while (!isStopped) {
            try {
                perform();
            }
            catch (Exception e) {
                handleException(e);
            }
        }
        return null;
    }

    // Request to stop thread without blocking.
    public void stop()
    {
        this.isStopped = true;
    }

    // Perform one iteration of worker loop. This must be overridden in subclass.
    public void perform()
            throws Exception
    {
        throw new UnsupportedOperationException("run() method was not overridden");
    }

    private void handleException(Exception e)
    {
        Sentry.capture(e);
        LOG.error("Unhandled exception!: " + e.getMessage());
    }
}

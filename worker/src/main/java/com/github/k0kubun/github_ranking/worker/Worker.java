package com.github.k0kubun.github_ranking.worker;

import java.io.IOException;
import java.lang.UnsupportedOperationException;
import java.util.concurrent.Callable;

public abstract class Worker
        implements Callable<Void>
{
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
        System.err.println("Unhandled exception!: " + e.getMessage());
    }
}

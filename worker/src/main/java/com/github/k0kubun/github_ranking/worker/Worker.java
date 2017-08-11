package com.github.k0kubun.github_ranking.worker;

import java.lang.UnsupportedOperationException;
import java.util.concurrent.Callable;

public class Worker implements Callable<Void>
{
    private boolean isStopped;

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
            } catch (Exception e) {
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
    private void perform() throws Exception
    {
        throw new UnsupportedOperationException("run() method was not overridden");
    }

    private void handleException(Exception e)
    {
        // TODO: handle
    }
}

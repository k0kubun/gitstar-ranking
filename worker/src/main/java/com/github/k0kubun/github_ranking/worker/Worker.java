package com.github.k0kubun.github_ranking.worker;

public interface Worker
{
    // Start worker thread.
    public void start();

    // Request to stop thread without blocking.
    public void stop();

    // Blocks until thread is stopped. This should be called after `stop()`.
    public void waitUntilStop();
}

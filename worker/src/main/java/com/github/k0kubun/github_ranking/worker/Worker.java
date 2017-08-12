package com.github.k0kubun.github_ranking.worker;

import java.io.IOException;
import java.lang.UnsupportedOperationException;
import java.util.concurrent.Callable;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class Worker implements Callable<Void>
{
    private boolean isStopped;

    public static Logger buildLogger(String name)
    {
        return buildLogger("worker.log", name);
    }

    public static Logger buildLogger(String filename, String name)
    {
        Logger logger = Logger.getLogger(name);
        logger.setLevel(Level.FINE);

        try {
            FileHandler fileHandler = new FileHandler(System.getProperty("user.dir") + "/log/" + filename);
            fileHandler.setFormatter(new SimpleFormatter());
            logger.addHandler(fileHandler);
        } catch (IOException e) {
            System.err.println("Failed to initialize logger!: " + e.getMessage());
        }
        return logger;
    }

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
    public void perform() throws Exception
    {
        throw new UnsupportedOperationException("run() method was not overridden");
    }

    private void handleException(Exception e)
    {
        System.err.println("Perform failed!: " + e.getMessage());
    }
}

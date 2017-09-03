package com.github.k0kubun.github_ranking.worker;

import com.github.k0kubun.github_ranking.config.Config;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NewUserWorker
        extends Worker
{
    private static final Logger LOG = LoggerFactory.getLogger(NewUserWorker.class);

    private final BlockingQueue<Boolean> newUserQueue;
    private final DBI dbi;

    public NewUserWorker(Config config)
    {
        super();
        newUserQueue = config.getQueueConfig().getUserRankingQueue();
        dbi = new DBI(config.getDatabaseConfig().getDataSource());
    }

    @Override
    public void perform()
            throws Exception
    {
        while (newUserQueue.poll(5, TimeUnit.SECONDS) == null) {
            if (isStopped) {
                return;
            }
        }
        LOG.info("----- started NewUserWorker -----");
        try (Handle handle = dbi.open()) {
            // TODO: implement
        }
        LOG.info("----- finished NewUserWorker -----");
    }
}

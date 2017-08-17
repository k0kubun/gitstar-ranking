package com.github.k0kubun.github_ranking.worker;

import com.github.k0kubun.github_ranking.config.Config;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserRankingWorker extends Worker
{
    private static final Logger LOG = LoggerFactory.getLogger(UserRankingWorker.class);

    private final BlockingQueue<Boolean> userRankingQueue;

    public UserRankingWorker(Config config)
    {
        super();
        userRankingQueue = config.getQueueConfig().getUserRankingQueue();
    }

    @Override
    public void perform() throws Exception
    {
        while (userRankingQueue.poll(5, TimeUnit.SECONDS) == null) {
            if (isStopped) {
                return;
            }
        }
        LOG.info("----- started UserRankingWorker -----");
        updateUserRanking();
        LOG.info("----- finished UserRankingWorker -----");
    }

    private void updateUserRanking()
    {
        // TODO: implement
    }
}

package com.github.k0kubun.github_ranking.config;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

// Its queue may be configurable in the future. So it's placed here but currently just a group of queues.
public class QueueConfig {
    private final BlockingQueue<Boolean> userRankingQueue;
    private final BlockingQueue<Boolean> orgRankingQueue;
    private final BlockingQueue<Boolean> repoRankingQueue;
    private final BlockingQueue<Boolean> userFullScanQueue;

    public QueueConfig() {
        userRankingQueue = new LinkedBlockingQueue<>();
        orgRankingQueue = new LinkedBlockingQueue<>();
        repoRankingQueue = new LinkedBlockingQueue<>();
        userFullScanQueue = new LinkedBlockingQueue<>();
    }

    public BlockingQueue<Boolean> getUserRankingQueue() {
        return userRankingQueue;
    }

    public BlockingQueue<Boolean> getOrgRankingQueue() {
        return orgRankingQueue;
    }

    public BlockingQueue<Boolean> getRepoRankingQueue() {
        return repoRankingQueue;
    }

    public BlockingQueue<Boolean> getUserFullScanQueue() {
        return userFullScanQueue;
    }
}

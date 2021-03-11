package com.github.k0kubun.gitstar_ranking.config

import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue

// Its queue may be configurable in the future. So it's placed here but currently just a group of queues.
class QueueConfig {
    val userRankingQueue: BlockingQueue<Boolean>
    val orgRankingQueue: BlockingQueue<Boolean>
    val repoRankingQueue: BlockingQueue<Boolean>
    val userStarScanQueue: BlockingQueue<Boolean>
    val userFullScanQueue: BlockingQueue<Boolean>

    init {
        userRankingQueue = LinkedBlockingQueue()
        orgRankingQueue = LinkedBlockingQueue()
        repoRankingQueue = LinkedBlockingQueue()
        userStarScanQueue = LinkedBlockingQueue()
        userFullScanQueue = LinkedBlockingQueue()
    }
}

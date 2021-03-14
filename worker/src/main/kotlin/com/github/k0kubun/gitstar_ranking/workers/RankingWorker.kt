package com.github.k0kubun.gitstar_ranking.workers

import com.github.k0kubun.gitstar_ranking.GitstarRankingConfiguration
import java.util.concurrent.BlockingQueue
import java.util.concurrent.TimeUnit

// Execute ranking workers sequentially to avoid pressuring DB by running them concurrently
class RankingWorker(config: GitstarRankingConfiguration) : Worker() {
    private val rankingQueue: BlockingQueue<Boolean> = config.queue.rankingQueue

    private val userRankingWorker: UserRankingWorker = UserRankingWorker(config)
    private val organizationRankingWorker: OrganizationRankingWorker = OrganizationRankingWorker(config)
    private val repositoryRankingWorker: RepositoryRankingWorker = RepositoryRankingWorker(config)

    override fun perform() {
        while (rankingQueue.poll(5, TimeUnit.SECONDS) == null) {
            if (isStopped) {
                return
            }
        }
        userRankingWorker.perform()
        organizationRankingWorker.perform()
        repositoryRankingWorker.perform()
    }
}

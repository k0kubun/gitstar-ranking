package com.github.k0kubun.github_ranking.worker;

import com.github.k0kubun.github_ranking.config.Config;
import com.github.k0kubun.github_ranking.dao.repository.RepositoryDao;
import com.github.k0kubun.github_ranking.dao.repository.RepositoryRankDao;
import com.github.k0kubun.github_ranking.model.Repository;
import com.github.k0kubun.github_ranking.model.RepositoryRank;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RepositoryRankingWorker
        extends Worker
{
    private static final int ITERATE_MIN_STARS = 10;
    private static final Logger LOG = LoggerFactory.getLogger(RepositoryRankingWorker.class);

    private final BlockingQueue<Boolean> repoRankingQueue;
    private final DBI dbi;

    public RepositoryRankingWorker(Config config)
    {
        super();
        repoRankingQueue = config.getQueueConfig().getRepoRankingQueue();
        dbi = new DBI(config.getDatabaseConfig().getDataSource());
    }

    @Override
    public void perform()
            throws Exception
    {
        while (repoRankingQueue.poll(5, TimeUnit.SECONDS) == null) {
            if (isStopped) {
                return;
            }
        }
        LOG.info("----- started RepositoryRankingWorker -----");
        try (Handle handle = dbi.open()) {
            RepositoryRank lastRank = updateUpperRanking(handle);
            if (lastRank != null) {
                updateLowerRanking(handle, lastRank);
            }
        }
        LOG.info("----- finished RepositoryRankingWorker -----");
    }

    private RepositoryRank updateUpperRanking(Handle handle)
    {
        int count = handle.attach(RepositoryDao.class).countRepos(); // warmup
        PaginatedRepositories paginatedRepos = new PaginatedRepositories(handle);
        List<Repository> repos;

        List<RepositoryRank> commitPendingRanks = new ArrayList<>(); // listed in stargazers_count DESC
        RepositoryRank currentRank = null;
        int currentRankNum = 0;

        while (!(repos = paginatedRepos.nextRepos()).isEmpty()) {
            // Shutdown immediately if requested, even if it's in progress.
            if (isStopped) {
                return null;
            }

            for (Repository repo : repos) {
                if (currentRank == null) {
                    currentRank = new RepositoryRank(repo.getStargazersCount(), 1);
                    currentRankNum = 1;
                }
                else if (currentRank.getStargazersCount() == repo.getStargazersCount()) {
                    currentRankNum++;
                }
                else {
                    commitPendingRanks.add(currentRank);
                    currentRank = new RepositoryRank(repo.getStargazersCount(), currentRank.getRank() + currentRankNum);
                    currentRankNum = 1;
                }
            }

            if (!commitPendingRanks.isEmpty()) {
                commitRanks(handle, commitPendingRanks);
                commitPendingRanks.clear();
            }
            int rows = currentRank.getRank() + currentRankNum - 1;
            LOG.info("RepositoryRankingWorker (" + calcProgress(rows, count) + ", " + Integer.valueOf(rows).toString() +
                    "/" + Integer.valueOf(count).toString() + " rows, rank " + Integer.valueOf(currentRank.getRank()).toString() + ", " +
                    Integer.valueOf(currentRank.getStargazersCount()).toString() + " stars)");

            // Switch the way to calculate ranking under 10 stars
            if (currentRank.getStargazersCount() <= ITERATE_MIN_STARS) {
                return currentRank;
            }
        }
        return currentRank;
    }

    private void updateLowerRanking(Handle handle, RepositoryRank lastRepoRank)
    {
        List<RepositoryRank> repoRanks = new ArrayList<>(); // listed in stargazers_count DESC
        repoRanks.add(lastRepoRank);

        int lastRank = lastRepoRank.getRank();
        for (int lastStars = lastRepoRank.getStargazersCount(); lastStars > 0; lastStars--) {
            LOG.info("RepositoryRankingWorker for " + Integer.valueOf(lastStars - 1).toString());
            int count = handle.attach(RepositoryDao.class).countReposHavingStars(lastStars);
            repoRanks.add(new RepositoryRank(lastStars - 1, lastRank + count));
            lastRank += count;
        }
        commitRanks(handle, repoRanks);
    }

    private void commitRanks(Handle handle, List<RepositoryRank> repoRanks)
    {
        // `repoRanks` is listed in stargazers_count DESC
        Integer minStars = lastOf(repoRanks).getStargazersCount();
        Integer maxStars = repoRanks.get(0).getStargazersCount();

        handle.useTransaction((conn, status) -> {
            conn.attach(RepositoryRankDao.class).deleteBetween(minStars, maxStars);
            conn.attach(RepositoryRankDao.class).bulkInsert(repoRanks);
        });
    }

    private RepositoryRank lastOf(List<RepositoryRank> repoRanks)
    {
        return repoRanks.get(repoRanks.size() - 1);
    }

    private String calcProgress(int child, int parent)
    {
        return String.format("%.3f%%", (float) child / (float) parent);
    }

    // This class does cursor-based-pagination for repositories order by stargazers_count DESC.
    public class PaginatedRepositories
    {
        private static final int PAGE_SIZE = 5000;

        private final RepositoryDao repoDao;
        private Integer lastMinStars;
        private Long lastMinId;

        public PaginatedRepositories(Handle handle)
        {
            repoDao = handle.attach(RepositoryDao.class);
            lastMinStars = null;
            lastMinId = null;
        }

        public List<Repository> nextRepos()
        {
            List<Repository> repos;
            if (lastMinId == null && lastMinStars == null) {
                repos = repoDao.starsDescFirstRepos(PAGE_SIZE);
            }
            else {
                repos = repoDao.starsDescReposAfter(lastMinStars, lastMinId, PAGE_SIZE);
            }
            if (repos.isEmpty()) {
                return repos;
            }

            Repository lastRepo = repos.get(repos.size() - 1);
            lastMinStars = lastRepo.getStargazersCount();
            lastMinId = lastRepo.getId();
            return repos;
        }
    }
}

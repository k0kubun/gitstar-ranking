package com.github.k0kubun.github_ranking.worker;

import com.github.k0kubun.github_ranking.config.Config;
import com.github.k0kubun.github_ranking.repository.dao.OrganizationDao;
import com.github.k0kubun.github_ranking.repository.dao.OrganizationRankDao;
import com.github.k0kubun.github_ranking.model.Organization;
import com.github.k0kubun.github_ranking.model.OrganizationRank;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OrganizationRankingWorker
        extends Worker
{
    private static final int ITERATE_MIN_STARS = 10;
    private static final Logger LOG = LoggerFactory.getLogger(OrganizationRankingWorker.class);

    private final BlockingQueue<Boolean> orgRankingQueue;
    private final DBI dbi;

    public OrganizationRankingWorker(Config config)
    {
        super();
        orgRankingQueue = config.getQueueConfig().getOrgRankingQueue();
        dbi = new DBI(config.getDatabaseConfig().getDataSource());
    }

    @Override
    public void perform()
            throws Exception
    {
        while (orgRankingQueue.poll(5, TimeUnit.SECONDS) == null) {
            if (isStopped) {
                return;
            }
        }
        LOG.info("----- started OrganizationRankingWorker -----");
        try (Handle handle = dbi.open()) {
            OrganizationRank lastRank = updateUpperRanking(handle);
            if (lastRank != null) {
                updateLowerRanking(handle, lastRank);
            }
        }
        LOG.info("----- finished OrganizationRankingWorker -----");
    }

    private OrganizationRank updateUpperRanking(Handle handle)
    {
        int count = handle.attach(OrganizationDao.class).countOrganizations(); // warmup
        PaginatedOrganizations paginatedOrgs = new PaginatedOrganizations(handle);
        List<Organization> orgs;

        List<OrganizationRank> commitPendingRanks = new ArrayList<>(); // listed in stargazers_count DESC
        OrganizationRank currentRank = null;
        int currentRankNum = 0;

        while (!(orgs = paginatedOrgs.nextOrgs()).isEmpty()) {
            // Shutdown immediately if requested, even if it's in progress.
            if (isStopped) {
                return null;
            }

            for (Organization org : orgs) {
                if (currentRank == null) {
                    currentRank = new OrganizationRank(org.getStargazersCount(), 1);
                    currentRankNum = 1;
                }
                else if (currentRank.getStargazersCount() == org.getStargazersCount()) {
                    currentRankNum++;
                }
                else {
                    commitPendingRanks.add(currentRank);
                    currentRank = new OrganizationRank(org.getStargazersCount(), currentRank.getRank() + currentRankNum);
                    currentRankNum = 1;
                }
            }

            if (!commitPendingRanks.isEmpty()) {
                commitRanks(handle, commitPendingRanks);
                commitPendingRanks.clear();
            }
            int rows = currentRank.getRank() + currentRankNum - 1;
            LOG.info("OrganizationRankingWorker (" + calcProgress(rows, count) + ", " + Integer.valueOf(rows).toString() +
                    "/" + Integer.valueOf(count).toString() + " rows, rank " + Integer.valueOf(currentRank.getRank()).toString() + ", " +
                    Integer.valueOf(currentRank.getStargazersCount()).toString() + " stars)");

            // Switch the way to calculate ranking under 10 stars
            if (currentRank.getStargazersCount() <= ITERATE_MIN_STARS) {
                return currentRank;
            }
        }
        return currentRank;
    }

    private void updateLowerRanking(Handle handle, OrganizationRank lastOrgRank)
    {
        List<OrganizationRank> orgRanks = new ArrayList<>(); // listed in stargazers_count DESC
        orgRanks.add(lastOrgRank);

        int lastRank = lastOrgRank.getRank();
        for (int lastStars = lastOrgRank.getStargazersCount(); lastStars > 0; lastStars--) {
            LOG.info("OrganizationRankingWorker for " + Integer.valueOf(lastStars - 1).toString());
            int count = handle.attach(OrganizationDao.class).countOrganizationsHavingStars(lastStars);
            orgRanks.add(new OrganizationRank(lastStars - 1, lastRank + count));
            lastRank += count;
        }
        commitRanks(handle, orgRanks);
    }

    private void commitRanks(Handle handle, List<OrganizationRank> orgRanks)
    {
        // `orgRanks` is listed in stargazers_count DESC
        Integer minStars = lastOf(orgRanks).getStargazersCount();
        Integer maxStars = orgRanks.get(0).getStargazersCount();

        handle.useTransaction((conn, status) -> {
            conn.attach(OrganizationRankDao.class).deleteBetween(minStars, maxStars);
            conn.attach(OrganizationRankDao.class).bulkInsert(orgRanks);
        });
    }

    private OrganizationRank lastOf(List<OrganizationRank> orgRanks)
    {
        return orgRanks.get(orgRanks.size() - 1);
    }

    private String calcProgress(int child, int parent)
    {
        return String.format("%.3f%%", (float) child / (float) parent);
    }

    // This class does cursor-based-pagination for organizations order by stargazers_count DESC.
    public class PaginatedOrganizations
    {
        private static final int PAGE_SIZE = 5000;

        private final OrganizationDao orgDao;
        private Integer lastMinStars;
        private Integer lastMinId;

        public PaginatedOrganizations(Handle handle)
        {
            orgDao = handle.attach(OrganizationDao.class);
            lastMinStars = null;
            lastMinId = null;
        }

        public List<Organization> nextOrgs()
        {
            List<Organization> orgs;
            if (lastMinId == null && lastMinStars == null) {
                orgs = orgDao.starsDescFirstOrgs(PAGE_SIZE);
            }
            else {
                orgs = orgDao.starsDescOrgsAfter(lastMinStars, lastMinId, PAGE_SIZE);
            }
            if (orgs.isEmpty()) {
                return orgs;
            }

            Organization lastOrg = orgs.get(orgs.size() - 1);
            lastMinStars = lastOrg.getStargazersCount();
            lastMinId = lastOrg.getId();
            return orgs;
        }
    }
}

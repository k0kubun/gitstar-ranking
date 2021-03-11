package com.github.k0kubun.gitstar_ranking.repository;

import com.github.k0kubun.gitstar_ranking.model.Organization;
import com.github.k0kubun.gitstar_ranking.repository.dao.OrganizationDao;

import java.util.List;

import org.skife.jdbi.v2.Handle;

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

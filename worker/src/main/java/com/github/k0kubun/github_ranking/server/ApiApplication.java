package com.github.k0kubun.github_ranking.server;

import com.github.k0kubun.github_ranking.api.AvailabilityResource;
import com.github.k0kubun.github_ranking.api.RankingResource;
import com.treasuredata.underwrap.UnderwrapServer;
import java.util.HashSet;
import java.util.Set;
import javax.ws.rs.core.Application;

public class ApiApplication extends UnderwrapServer.UnderwrapApplication
{
    @Override
    protected void registerResources(Set<Class<?>> classes)
    {
        classes.add(AvailabilityResource.class);
        classes.add(RankingResource.class);
    }
}

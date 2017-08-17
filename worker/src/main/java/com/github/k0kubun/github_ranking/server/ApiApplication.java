package com.github.k0kubun.github_ranking.server;

import com.github.k0kubun.github_ranking.api.AvailabilityResource;
import com.github.k0kubun.github_ranking.api.RankingResource;
import java.util.HashSet;
import java.util.Set;
import javax.ws.rs.core.Application;

public class ApiApplication extends Application
{
    @Override
    public Set<Class<?>> getClasses()
    {
        HashSet<Class<?>> classes = new HashSet<>();
        classes.add(AvailabilityResource.class);
        classes.add(RankingResource.class);
        return classes;
    }
}

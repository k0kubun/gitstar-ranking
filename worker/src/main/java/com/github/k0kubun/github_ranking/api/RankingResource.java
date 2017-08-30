package com.github.k0kubun.github_ranking.api;

import com.github.k0kubun.github_ranking.config.QueueConfig;
import com.github.k0kubun.github_ranking.model.ApiResponse;

import java.sql.Time;
import java.util.concurrent.BlockingQueue;

import javax.sql.DataSource;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/ranks")
@Produces("application/json")
public class RankingResource
{
    private static final Logger LOG = LoggerFactory.getLogger(RankingResource.class);

    private final QueueConfig queueConfig;

    public RankingResource(@Context QueueConfig queueConfig)
    {
        this.queueConfig = queueConfig;
    }

    @POST
    @Path("/users")
    public ApiResponse calcUsers()
    {
        return new ApiResponse<>(ApiResponse.Type.INTEGER,
                scheduleIfEmpty(queueConfig.getUserRankingQueue()));
    }

    @POST
    @Path("/orgs")
    public ApiResponse calcOrgs()
    {
        return new ApiResponse<>(ApiResponse.Type.INTEGER,
                scheduleIfEmpty(queueConfig.getOrgRankingQueue()));
    }

    @POST
    @Path("/repos")
    public ApiResponse calcRepos()
    {
        return new ApiResponse<>(ApiResponse.Type.INTEGER,
                scheduleIfEmpty(queueConfig.getRepoRankingQueue()));
    }

    private Integer scheduleIfEmpty(BlockingQueue<Boolean> queue)
    {
        int size = queue.size();
        if (size == 0) {
            try {
                queue.put(true);
            }
            catch (InterruptedException e) {
                LOG.error("API queueing interrupted: " + e.getMessage());
            }
        }
        return size;
    }
}

package com.github.k0kubun.github_ranking.api;

import com.github.k0kubun.github_ranking.config.QueueConfig;
import com.github.k0kubun.github_ranking.model.ApiResponse;
import io.sentry.Sentry;

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

@Path("/jobs")
@Produces("application/json")
public class JobResource
{
    private static final Logger LOG = LoggerFactory.getLogger(JobResource.class);

    private final QueueConfig queueConfig;

    public JobResource(@Context QueueConfig queueConfig)
    {
        this.queueConfig = queueConfig;
    }

    @POST
    @Path("/user_ranks")
    public ApiResponse calcUsers()
    {
        return new ApiResponse<>(ApiResponse.Type.INTEGER,
                scheduleIfEmpty(queueConfig.getUserRankingQueue()));
    }

    @POST
    @Path("/org_ranks")
    public ApiResponse calcOrgs()
    {
        return new ApiResponse<>(ApiResponse.Type.INTEGER,
                scheduleIfEmpty(queueConfig.getOrgRankingQueue()));
    }

    @POST
    @Path("/repo_ranks")
    public ApiResponse calcRepos()
    {
        return new ApiResponse<>(ApiResponse.Type.INTEGER,
                scheduleIfEmpty(queueConfig.getRepoRankingQueue()));
    }

    @POST
    @Path("/new_users")
    public ApiResponse newUsers()
    {
        return new ApiResponse<>(ApiResponse.Type.INTEGER,
                scheduleIfEmpty(queueConfig.getNewUserQueue()));
    }

    @POST
    @Path("/searched_users")
    public ApiResponse searchedUsers()
    {
        return new ApiResponse<>(ApiResponse.Type.INTEGER,
                scheduleIfEmpty(queueConfig.getSearchedUserQueue()));
    }

    @POST
    @Path("/full_scan")
    public ApiResponse fullScan()
    {
        return new ApiResponse<>(ApiResponse.Type.INTEGER,
                scheduleIfEmpty(queueConfig.getUserFullScanQueue()));
    }

    private Integer scheduleIfEmpty(BlockingQueue<Boolean> queue)
    {
        int size = queue.size();
        if (size == 0) {
            try {
                queue.put(true);
            }
            catch (InterruptedException e) {
                Sentry.capture(e);
                LOG.error("API queueing interrupted: " + e.getMessage());
            }
        }
        return size;
    }
}

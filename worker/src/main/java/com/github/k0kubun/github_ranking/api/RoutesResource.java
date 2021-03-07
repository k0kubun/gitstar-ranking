package com.github.k0kubun.github_ranking.api;

import com.github.k0kubun.github_ranking.model.ApiResponse;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

@Path("/")
@Produces({"application/json"})
public class RoutesResource
{
    @GET
    @Path("/")
    public static ApiResponse getRoutes()
    {
        List<String> routes = new ArrayList<>();
        routes.add("GET /");
        routes.add("POST /jobs/user_ranks");
        routes.add("POST /jobs/org_ranks");
        routes.add("POST /jobs/repo_ranks");
        routes.add("POST /jobs/full_scan");
        return new ApiResponse<>(ApiResponse.Type.ARRAY, routes);
    }
}

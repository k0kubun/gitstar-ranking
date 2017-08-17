package com.github.k0kubun.github_ranking.api;

import com.github.k0kubun.github_ranking.model.ApiResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

@Path("/")
@Produces({"application/json"})
public class AvailabilityResource
{
    @GET
    @Path("/")
    public static ApiResponse getAvailability()
    {
        return new ApiResponse<>(ApiResponse.Type.BOOLEAN, true);
    }
}

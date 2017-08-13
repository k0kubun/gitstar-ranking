package com.github.k0kubun.github_ranking.model;

public class UpdateUserJob
{
    private final Integer id;
    private final Integer userId;

    public UpdateUserJob(Integer id, Integer userId)
    {
        this.id = id;
        this.userId = userId;
    }

    public Integer getId()
    {
        return id;
    }

    public Integer getUserId()
    {
        return userId;
    }
}

package com.github.k0kubun.github_ranking.model;

public class UpdateUserJob
{
    private final Integer id;
    private final Integer userId;
    private final Integer tokenUserId;

    public UpdateUserJob(Integer id, Integer userId, Integer tokenUserId)
    {
        this.id = id;
        this.userId = userId;
        this.tokenUserId = tokenUserId;
    }

    public Integer getId()
    {
        return id;
    }

    public Integer getUserId()
    {
        return userId;
    }

    public Integer getTokenUserId()
    {
        return tokenUserId;
    }
}

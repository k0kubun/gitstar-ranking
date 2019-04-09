package com.github.k0kubun.github_ranking.model;

public class UpdateUserJob
{
    private final Integer id;
    private final String userId;
    private final String userName;
    private final Long tokenUserId;

    public UpdateUserJob(Integer id, String userId, String userName, String tokenUserId)
    {
        this.id = id;
        // either userId or userName is specified in payload.
        this.userId = userId;
        this.userName = userName;
        this.tokenUserId = Long.valueOf(tokenUserId);
    }

    public Integer getId()
    {
        return id;
    }

    public Long getUserId()
    {
        return Long.valueOf(userId);
    }

    public String getUserName() { return userName; }

    public Long getTokenUserId()
    {
        return Long.valueOf(tokenUserId);
    }
}

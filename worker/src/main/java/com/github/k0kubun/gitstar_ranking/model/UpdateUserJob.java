package com.github.k0kubun.github_ranking.model;

public class UpdateUserJob
{
    private final Integer id;
    private final Long userId;
    private final String userName;
    private final Long tokenUserId;

    public UpdateUserJob(Integer id, Long userId, String userName, Long tokenUserId)
    {
        this.id = id;
        // either userId or userName is specified in payload.
        this.userId = (userId == 0 ? null : userId);
        this.userName = userName;
        this.tokenUserId = tokenUserId;
    }

    public Integer getId()
    {
        return id;
    }

    public Long getUserId()
    {
        return userId;
    }

    public String getUserName() {
        return userName;
    }

    public Long getTokenUserId()
    {
        return Long.valueOf(tokenUserId);
    }
}

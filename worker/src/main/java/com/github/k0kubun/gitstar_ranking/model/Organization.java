package com.github.k0kubun.gitstar_ranking.model;

import java.sql.Timestamp;

public class Organization
{
    private final int id;
    private final String login;
    private final int stargazersCount;
    private final Timestamp updatedAt;

    public Organization(int id, String login, int stargazersCount, Timestamp updatedAt)
    {
        this.id = id;
        this.login = login;
        this.stargazersCount = stargazersCount;
        this.updatedAt = updatedAt;
    }

    public Integer getId()
    {
        return id;
    }

    public int getStargazersCount()
    {
        return stargazersCount;
    }

    public Timestamp getUpdatedAt()
    {
        return updatedAt;
    }

    public User toUser()
    {
        User user = new User(id, "Organization");
        user.setLogin(login);
        user.setStargazersCount(stargazersCount);
        user.setUpdatedAt(updatedAt);
        return user;
    }
}

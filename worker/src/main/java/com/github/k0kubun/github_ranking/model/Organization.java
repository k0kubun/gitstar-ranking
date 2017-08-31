package com.github.k0kubun.github_ranking.model;

public class Organization
{
    private final int id;
    private final String login;
    private final int stargazersCount;

    public Organization(int id, String login, int stargazersCount)
    {
        this.id = id;
        this.login = login;
        this.stargazersCount = stargazersCount;
    }

    public Integer getId()
    {
        return id;
    }

    public int getStargazersCount()
    {
        return stargazersCount;
    }

    public User toUser()
    {
        User user = new User(id, "Organization");
        user.setLogin(login);
        user.setStargazersCount(stargazersCount);
        return user;
    }
}

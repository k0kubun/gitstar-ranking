package com.github.k0kubun.github_ranking.model;

public class Organization
{
    private final int id;
    private final int stargazersCount;

    public Organization(int id, int stargazersCount)
    {
        this.id = id;
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
}

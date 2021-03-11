package com.github.k0kubun.gitstar_ranking.model;

public class UserRank
{
    private final int stargazersCount;
    private final int rank;

    public UserRank(int stargazersCount, int rank)
    {
        this.stargazersCount = stargazersCount;
        this.rank = rank;
    }

    public int getStargazersCount()
    {
        return stargazersCount;
    }

    public int getRank()
    {
        return rank;
    }
}

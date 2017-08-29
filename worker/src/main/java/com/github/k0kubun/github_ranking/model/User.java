package com.github.k0kubun.github_ranking.model;

public class User
{
    private final int id;
    private final String type;
    private String login;
    private int stargazersCount;

    public User(int id, String type)
    {
        this.id = id;
        this.type = type;
    }

    public Integer getId()
    {
        return id;
    }

    public String getLogin()
    {
        return login;
    }

    public void setLogin(String login)
    {
        this.login = login;
    }

    public int getStargazersCount()
    {
        return stargazersCount;
    }

    public void setStargazersCount(Integer stargazersCount)
    {
        this.stargazersCount = stargazersCount;
    }

    public boolean isOrganization()
    {
        return type.equals("Organization");
    }
}

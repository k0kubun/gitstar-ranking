package com.github.k0kubun.github_ranking.model;

public class User
{
    private final Integer id;
    private final String type;
    private String login;
    private Integer stargazersCount;

    public User(Integer id, String type)
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

    public Integer getStargazersCount()
    {
        return stargazersCount;
    }

    public void setStargazersCount(Integer stargazersCount)
    {
        this.stargazersCount = stargazersCount;
    }

    public boolean isOrganization()
    {
        return type == "Organization";
    }
}

package com.github.k0kubun.github_ranking.model;

public class User
{
    private final int id;
    private final String type;
    private String login;
    private int stargazersCount;
    private String avatarUrl;

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

    public String getType()
    {
        return type;
    }

    public int getStargazersCount()
    {
        return stargazersCount;
    }

    public void setStargazersCount(Integer stargazersCount)
    {
        this.stargazersCount = stargazersCount;
    }

    public void setAvatarUrl(String avatarUrl)
    {
        this.avatarUrl = avatarUrl;
    }

    public String getAvatarUrl()
    {
        return avatarUrl;
    }

    public boolean isOrganization()
    {
        return type.equals("Organization");
    }
}

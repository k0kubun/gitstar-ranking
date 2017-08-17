package com.github.k0kubun.github_ranking.model;

public class User
{
    private final Integer id;
    private final String login;
    private final String type;

    public User(Integer id, String login, String type)
    {
        this.id = id;
        this.login = login;
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

    public boolean isOrganization()
    {
        return type == "Organization";
    }
}

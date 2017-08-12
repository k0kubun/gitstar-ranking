package com.github.k0kubun.github_ranking.model;

public class User
{
    private final Integer id;
    private final String login;

    public User(Integer id, String login)
    {
        this.id = id;
        this.login = login;
    }

    public Integer getId()
    {
        return id;
    }

    public String getLogin()
    {
        return login;
    }
}

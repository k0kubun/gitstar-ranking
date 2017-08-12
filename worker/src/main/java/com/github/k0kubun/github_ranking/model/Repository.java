package com.github.k0kubun.github_ranking.model;

public class Repository
{
    private final Integer id;
    private final String name;

    public Repository(Integer id, String name)
    {
        this.id = id;
        this.name = name;
    }

    public Integer getId()
    {
        return id;
    }

    public String getName()
    {
        return name;
    }
}

package com.github.k0kubun.gitstar_ranking.model;

import java.util.List;
import java.util.ArrayList;

public class Repository
{
    private final Long id;
    private final Integer ownerId;
    private final String name;
    private final String fullName;
    private final String description;
    private final Boolean fork;
    private final String homepage;
    private final int stargazersCount;
    private final String language;

    public Repository(Long id, int stargazersCount)
    {
        this.id = id;
        this.stargazersCount = stargazersCount;
        this.ownerId = null;
        this.name = null;
        this.fullName = null;
        this.description = null;
        this.fork = null;
        this.homepage = null;
        this.language = null;
    }

    public Repository(Long id, Integer ownerId, String name, String fullName, String description,
            Boolean fork, String homepage, int stargazersCount, String language)
    {
        this.id = id;
        this.ownerId = ownerId;
        this.name = name;
        this.fullName = fullName;
        this.description = description;
        this.fork = fork;
        this.homepage = homepage;
        this.stargazersCount = stargazersCount;
        this.language = language;
    }

    public Long getId()
    {
        return id;
    }

    public Integer getOwnerId()
    {
        return ownerId;
    }

    public String getName()
    {
        return name;
    }

    public String getFullName()
    {
        return fullName;
    }

    public String getDescription()
    {
        return description;
    }

    public Boolean getFork()
    {
        return fork;
    }

    public String getHomepage()
    {
        return homepage;
    }

    public int getStargazersCount()
    {
        return stargazersCount;
    }

    public String getLanguage()
    {
        return language;
    }
}

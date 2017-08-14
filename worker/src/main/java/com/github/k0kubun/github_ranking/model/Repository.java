package com.github.k0kubun.github_ranking.model;

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
    private final Integer stargazersCount;
    private final String language;

    public static List<Repository> fromGitHubRepos(List<org.eclipse.egit.github.core.Repository> repos, String login)
    {
        List<Repository> result = new ArrayList<Repository>();
        for (org.eclipse.egit.github.core.Repository repo : repos) {
            result.add(fromGitHubRepo(repo, login));
        }
        return result;
    }

    public static Repository fromGitHubRepo(org.eclipse.egit.github.core.Repository repo, String login)
    {
        return new Repository(
                repo.getId(),
                repo.getOwner().getId(),
                repo.getName(),
                login + "/" + repo.getName(),
                repo.getDescription(),
                repo.isFork(),
                repo.getHomepage(),
                repo.getWatchers(), // stargazers_count
                repo.getLanguage()
        );
    }

    public Repository(Long id, Integer ownerId, String name, String fullName, String description,
            Boolean fork, String homepage, Integer stargazersCount, String language)
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

    public Integer getStargazersCount()
    {
        return stargazersCount;
    }

    public String getLanguage()
    {
        return language;
    }
}

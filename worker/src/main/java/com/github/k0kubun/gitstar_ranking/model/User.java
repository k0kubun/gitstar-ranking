package com.github.k0kubun.gitstar_ranking.model;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class User
{
    private final long id;
    private final String type;
    private String login;
    private int stargazersCount;
    private String avatarUrl;
    private Timestamp updatedAt;

    public User(long id, String type)
    {
        this.id = id;
        this.type = type;
        this.updatedAt = null;
    }

    public Long getId()
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

    public void setUpdatedAt(Timestamp updatedAt)
    {
        this.updatedAt = updatedAt;
    }

    public Timestamp getUpdatedAt()
    {
        return updatedAt;
    }

    public boolean isOrganization()
    {
        return type.equals("Organization");
    }

    public boolean isUpdatedWithinDays(long days)
    {
        LocalDateTime daysAgo = LocalDateTime.now(ZoneId.of("UTC")).minusDays(days);
        return updatedAt.after(Timestamp.valueOf(daysAgo));
    }
}

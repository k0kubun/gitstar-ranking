package com.github.k0kubun.github_ranking.github;

public class StaticTokenFactory
        implements AccessTokenFactory
{
    private final String accessToken;

    public StaticTokenFactory(String accessToken)
    {
        this.accessToken = accessToken;
    }

    @Override
    public String getToken()
    {
        return accessToken;
    }
}

package com.github.k0kubun.github_ranking.repository.dao;

import com.github.k0kubun.github_ranking.model.Organization;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.Mapper;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

public interface OrganizationDao
{
    @SqlQuery("select id, login, type, stargazers_count, updated_at from users where type = 'Organization' order by stargazers_count desc, id desc limit :limit")
    @Mapper(OrganizationMapper.class)
    List<Organization> starsDescFirstOrgs(@Bind("limit") Integer limit);

    @SqlQuery("select id, login, type, stargazers_count, updated_at from users where type = 'Organization' and " +
            "(stargazers_count, id) < (:stargazersCount, :id) order by stargazers_count desc, id desc limit :limit")
    @Mapper(OrganizationMapper.class)
    List<Organization> starsDescOrgsAfter(@Bind("stargazersCount") Integer stargazersCount, @Bind("id") Integer id, @Bind("limit") Integer limit);

    @SqlQuery("select count(1) from users where type = 'Organization'")
    int countOrganizations();

    @SqlQuery("select count(1) from users where type = 'Organization' and stargazers_count = :stargazersCount")
    int countOrganizationsHavingStars(@Bind("stargazersCount") int stargazersCount);

    class OrganizationMapper
            implements ResultSetMapper<Organization>
    {
        @Override
        public Organization map(int index, ResultSet r, StatementContext ctx)
                throws SQLException
        {
            return new Organization(r.getInt("id"), r.getString("login"), r.getInt("stargazers_count"), r.getTimestamp("updated_at"));
        }
    }
}

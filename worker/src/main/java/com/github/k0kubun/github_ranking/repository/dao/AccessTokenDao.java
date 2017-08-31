package com.github.k0kubun.github_ranking.repository.dao;

import com.github.k0kubun.github_ranking.model.AccessToken;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.customizers.Mapper;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

public interface AccessTokenDao
{
    @SqlQuery("select token from access_tokens where id = :id")
    @Mapper(AccessTokenMapper.class)
    AccessToken find(@Bind("id") Integer id);

    @SqlQuery("select token from access_tokens where user_id = :userId")
    @Mapper(AccessTokenMapper.class)
    AccessToken findByUserId(@Bind("userId") Integer userId);

    class AccessTokenMapper
            implements ResultSetMapper<AccessToken>
    {
        @Override
        public AccessToken map(int index, ResultSet r, StatementContext ctx)
                throws SQLException
        {
            return new AccessToken(
                    r.getString("token")
            );
        }
    }
}

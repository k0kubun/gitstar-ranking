package com.github.k0kubun.gitstar_ranking.repository.dao;

import com.github.k0kubun.gitstar_ranking.model.AccessToken;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

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
    AccessToken findByUserId(@Bind("userId") Long userId);

    // TODO: paginate
    @SqlQuery("select token from access_tokens where enabled = true limit 1000")
    @Mapper(AccessTokenMapper.class)
    List<AccessToken> allEnabledTokens();

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

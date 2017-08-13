package com.github.k0kubun.github_ranking.dao;

import com.github.k0kubun.github_ranking.model.Repository;
import com.github.k0kubun.github_ranking.model.User;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.customizers.Mapper;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

public interface RepositoryDao
{
    @SqlQuery("select id, login from users where id = :id")
    @Mapper(RepositoryMapper.class)
    User find(@Bind("id") Integer id);

    class RepositoryMapper implements ResultSetMapper<Repository>
    {
        @Override
        public Repository map(int index, ResultSet r, StatementContext ctx) throws SQLException
        {
            return new Repository(
                    r.getInt("id"),
                    r.getString("name")
            );
        }
    }
}

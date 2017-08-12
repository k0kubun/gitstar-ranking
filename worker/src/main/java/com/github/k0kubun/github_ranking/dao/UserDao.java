package com.github.k0kubun.github_ranking.dao;

import com.github.k0kubun.github_ranking.model.User;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.customizers.Mapper;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

public interface UserDao
{
    @SqlQuery("select id, login from users where id = :id")
    @Mapper(UserMapper.class)
    User find(@Bind("id") Integer id);

    class UserMapper implements ResultSetMapper<User>
    {
        @Override
        public User map(int index, ResultSet r, StatementContext ctx) throws SQLException
        {
            return new User(
                    r.getInt("id"),
                    r.getString("login")
            );
        }
    }
}

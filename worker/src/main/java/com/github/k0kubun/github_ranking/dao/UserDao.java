package com.github.k0kubun.github_ranking.dao;

import com.github.k0kubun.github_ranking.model.User;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.Mapper;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

public interface UserDao
{
    @SqlQuery("select id, login from users where id = :id")
    @Mapper(UserMapper.class)
    User find(@Bind("id") Integer id);

    @SqlUpdate("update users set stargazers_count = :stargazersCount, updated_at = current_timestamp() where id = :id")
    long updateStars(@Bind("id") Integer uuid, @Bind("stargazersCount") Integer stargazersCount);

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

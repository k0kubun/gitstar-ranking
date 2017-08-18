package com.github.k0kubun.github_ranking.dao;

import com.github.k0kubun.github_ranking.model.User;
import java.util.List;
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
    @SqlQuery("select id, login, type from users where id = :id")
    @Mapper(FindUserMapper.class)
    User find(@Bind("id") Integer id);

    @SqlUpdate("update users set stargazers_count = :stargazersCount, updated_at = current_timestamp() where id = :id")
    long updateStars(@Bind("id") Integer uuid, @Bind("stargazersCount") Integer stargazersCount);

    @SqlQuery("select id, type, stargazers_count from users where type = 'User' order by stargazers_count desc, id desc limit :limit")
    @Mapper(UserStarMapper.class)
    List<User> starsDescFirstUsers(@Bind("limit") Integer limit);

    @SqlQuery("select id, type, stargazers_count from users where type = 'User' and " +
              "(stargazers_count, id) < (:stargazersCount, :id) order by stargazers_count desc, id desc limit :limit")
    @Mapper(UserStarMapper.class)
    List<User> starsDescUsersAfter(@Bind("stargazersCount") Integer stargazersCount, @Bind("id") Integer id, @Bind("limit") Integer limit);

    class FindUserMapper implements ResultSetMapper<User>
    {
        @Override
        public User map(int index, ResultSet r, StatementContext ctx) throws SQLException
        {
            User user = new User(r.getInt("id"), r.getString("type"));
            user.setLogin(r.getString("login"));
            return user;
        }
    }

    class UserStarMapper implements ResultSetMapper<User>
    {
        @Override
        public User map(int index, ResultSet r, StatementContext ctx) throws SQLException
        {
            User user = new User(r.getInt("id"), r.getString("type"));
            user.setStargazersCount(r.getInt("stargazers_count"));
            return user;
        }
    }
}

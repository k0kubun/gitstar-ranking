package com.github.k0kubun.github_ranking.repository.dao;

import com.github.k0kubun.github_ranking.model.User;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.BindBean;
import org.skife.jdbi.v2.sqlobject.SqlBatch;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.BatchChunkSize;
import org.skife.jdbi.v2.sqlobject.customizers.Mapper;
import org.skife.jdbi.v2.sqlobject.stringtemplate.UseStringTemplate3StatementLocator;
import org.skife.jdbi.v2.tweak.ResultSetMapper;
import org.skife.jdbi.v2.unstable.BindIn;

@UseStringTemplate3StatementLocator
public interface UserDao
{
    @SqlQuery("select id, login, type from users where id = :id")
    @Mapper(UserMapper.class)
    User find(@Bind("id") Long id);

    @SqlQuery("select updated_at from users where id = :id")
    Timestamp userUpdatedAt(@Bind("id") Long id);

    @SqlQuery("select stargazers_count from users where id = :id")
    long userStargazersCount(@Bind("id") Long id);

    @SqlQuery("select stargazers_count from users order by stargazers_count desc limit 1;")
    long maxStargazersCount();

    @SqlQuery("select id, type, updated_at from users where id in (<ids>)")
    @Mapper(UserUpdatedAtMapper.class)
    List<User> findUsersWithUpdatedAt(@BindIn("ids") List<Long> ids);

    // This query does not update updated_at because updating it will show "Up to date" before updating repositories.
    @SqlUpdate("update users set login = :login where id = :id")
    long updateLogin(@Bind("id") Long id, @Bind("login") String login);

    @SqlUpdate("update users set stargazers_count = :stargazersCount, updated_at = current_timestamp(0) where id = :id")
    long updateStars(@Bind("id") Long id, @Bind("stargazersCount") Integer stargazersCount);

    @SqlQuery("select id from users order by id desc limit 1")
    long lastId();

    @SqlQuery("select id, login, type, stargazers_count, updated_at from users where type = 'User' order by stargazers_count desc, id desc limit :limit")
    @Mapper(UserStarMapper.class)
    List<User> starsDescFirstUsers(@Bind("limit") Integer limit);

    @SqlQuery("select id, login, type, stargazers_count, updated_at from users where type = 'User' and " +
            "(stargazers_count, id) \\< (:stargazersCount, :id) order by stargazers_count desc, id desc limit :limit")
    @Mapper(UserStarMapper.class)
    List<User> starsDescUsersAfter(@Bind("stargazersCount") Integer stargazersCount, @Bind("id") Long id, @Bind("limit") Integer limit);

    @SqlQuery("select id, login, type, stargazers_count, updated_at from users " +
            "where stargazers_count = :stargazersCount and id > :id order by id asc limit :limit")
    @Mapper(UserStarMapper.class)
    List<User> usersWithStarsAfter(@Bind("stargazersCount") long stargazersCount, @Bind("id") long id, @Bind("limit") int limit);

    @SqlQuery("select stargazers_count from users where stargazers_count < :stargazersCount order by stargazers_count desc limit 1")
    long nextStargazersCount(@Bind("stargazersCount") long stargazersCount);

    @SqlQuery("select count(1) from users where type = 'User'")
    int countUsers();

    @SqlQuery("select count(1) from users where type = 'User' and stargazers_count = :stargazersCount")
    int countUsersHavingStars(@Bind("stargazersCount") int stargazersCount);

    @SqlBatch("insert into users (id, type, login, avatar_url, created_at, updated_at) " +
            "values (:id, :type, :login, :avatarUrl, current_timestamp(0), current_timestamp(0)) " +
            "on conflict (id) do update set login=excluded.login, avatar_url=excluded.avatar_url") // DO NOT update updated_at on conflict for threshold check
    @BatchChunkSize(100)
    void bulkInsert(@BindBean List<User> users);

    @SqlUpdate("delete from users where id = :id")
    long delete(@Bind("id") Long id);

    class UserMapper
            implements ResultSetMapper<User>
    {
        @Override
        public User map(int index, ResultSet r, StatementContext ctx)
                throws SQLException
        {
            User user = new User(r.getLong("id"), r.getString("type"));
            user.setLogin(r.getString("login"));
            return user;
        }
    }

    class UserStarMapper
            implements ResultSetMapper<User>
    {
        @Override
        public User map(int index, ResultSet r, StatementContext ctx)
                throws SQLException
        {
            User user = new User(r.getInt("id"), r.getString("type"));
            user.setLogin(r.getString("login"));
            user.setStargazersCount(r.getInt("stargazers_count"));
            user.setUpdatedAt(r.getTimestamp("updated_at"));
            return user;
        }
    }

    class UserUpdatedAtMapper
            implements ResultSetMapper<User>
    {
        @Override
        public User map(int index, ResultSet r, StatementContext ctx)
                throws SQLException
        {
            User user = new User(r.getInt("id"), r.getString("type"));
            user.setUpdatedAt(r.getTimestamp("updated_at"));
            return user;
        }
    }
}

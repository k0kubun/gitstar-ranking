package com.github.k0kubun.gitstar_ranking.db

import com.github.k0kubun.gitstar_ranking.client.UserResponse
import com.github.k0kubun.gitstar_ranking.core.User
import java.sql.ResultSet
import java.sql.Timestamp
import org.skife.jdbi.v2.StatementContext
import org.skife.jdbi.v2.sqlobject.Bind
import org.skife.jdbi.v2.sqlobject.BindBean
import org.skife.jdbi.v2.sqlobject.SqlBatch
import org.skife.jdbi.v2.sqlobject.SqlQuery
import org.skife.jdbi.v2.sqlobject.SqlUpdate
import org.skife.jdbi.v2.sqlobject.customizers.BatchChunkSize
import org.skife.jdbi.v2.sqlobject.customizers.Mapper
import org.skife.jdbi.v2.sqlobject.stringtemplate.UseStringTemplate3StatementLocator
import org.skife.jdbi.v2.tweak.ResultSetMapper

@UseStringTemplate3StatementLocator
interface UserDao {
    @SqlQuery("select id, login, type, stargazers_count, updated_at from users where id = :id")
    @Mapper(UserStarMapper::class)
    fun find(@Bind("id") id: Long): User?

    @SqlQuery("select updated_at from users where id = :id")
    fun userUpdatedAt(@Bind("id") id: Long?): Timestamp?

    @SqlQuery("select stargazers_count from users order by stargazers_count desc limit 1;")
    fun maxStargazersCount(): Long

    // This query does not update updated_at because updating it will show "Up to date" before updating repositories.
    @SqlUpdate("update users set login = :login where id = :id")
    fun updateLogin(@Bind("id") id: Long?, @Bind("login") login: String?): Long

    @SqlUpdate("update users set stargazers_count = :stargazersCount, updated_at = current_timestamp(0) where id = :id")
    fun updateStars(@Bind("id") id: Long?, @Bind("stargazersCount") stargazersCount: Long?): Long

    @SqlQuery("select id from users order by id desc limit 1")
    fun lastId(): Long

    @SqlQuery("select id, login, type, stargazers_count, updated_at from users " +
        "where stargazers_count = :stargazersCount and :id \\< id order by id asc limit :limit")
    @Mapper(UserStarMapper::class)
    fun usersWithStarsAfter(@Bind("stargazersCount") stargazersCount: Long, @Bind("id") id: Long, @Bind("limit") limit: Int): List<User>

    @SqlQuery("select stargazers_count from users where stargazers_count \\< :stargazersCount order by stargazers_count desc limit 1")
    fun nextStargazersCount(@Bind("stargazersCount") stargazersCount: Long): Long

    @SqlBatch("insert into users (id, type, login, avatar_url, created_at, updated_at) " +
        "values (:id, :type, :login, :avatarUrl, current_timestamp(0), current_timestamp(0)) " +
        "on conflict (id) do update set login=excluded.login, avatar_url=excluded.avatar_url") // DO NOT update updated_at on conflict for threshold check
    @BatchChunkSize(100)
    fun bulkInsert(@BindBean users: List<UserResponse>)

    @SqlUpdate("delete from users where id = :id")
    fun delete(@Bind("id") id: Long?): Long

    class UserStarMapper : ResultSetMapper<User> {
        override fun map(index: Int, r: ResultSet, ctx: StatementContext): User {
            return User(
                id = r.getInt("id").toLong(),
                type = r.getString("type"),
                login = r.getString("login"),
                stargazersCount = r.getLong("stargazers_count"),
                updatedAt = r.getTimestamp("updated_at"),
            )
        }
    }
}

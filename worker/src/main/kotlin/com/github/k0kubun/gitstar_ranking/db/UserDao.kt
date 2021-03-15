package com.github.k0kubun.gitstar_ranking.db

import java.sql.Timestamp
import org.skife.jdbi.v2.sqlobject.Bind
import org.skife.jdbi.v2.sqlobject.SqlQuery
import org.skife.jdbi.v2.sqlobject.SqlUpdate
import org.skife.jdbi.v2.sqlobject.stringtemplate.UseStringTemplate3StatementLocator

@UseStringTemplate3StatementLocator
interface UserDao {
    @SqlQuery("select updated_at from users where id = :id")
    fun userUpdatedAt(@Bind("id") id: Long?): Timestamp?

    @SqlQuery("select stargazers_count from users order by stargazers_count desc limit 1;")
    fun maxStargazersCount(): Long

    @SqlQuery("select id from users order by id desc limit 1")
    fun lastId(): Long

    @SqlQuery("select stargazers_count from users where stargazers_count \\< :stargazersCount order by stargazers_count desc limit 1")
    fun nextStargazersCount(@Bind("stargazersCount") stargazersCount: Long): Long

    @SqlUpdate("delete from users where id = :id")
    fun delete(@Bind("id") id: Long?): Long
}

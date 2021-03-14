package com.github.k0kubun.gitstar_ranking.db

import com.github.k0kubun.gitstar_ranking.core.Repository
import org.skife.jdbi.v2.sqlobject.stringtemplate.UseStringTemplate3StatementLocator
import org.skife.jdbi.v2.sqlobject.SqlQuery
import org.skife.jdbi.v2.sqlobject.Bind
import org.skife.jdbi.v2.sqlobject.SqlBatch
import org.skife.jdbi.v2.sqlobject.customizers.BatchChunkSize
import org.skife.jdbi.v2.sqlobject.BindBean
import org.skife.jdbi.v2.sqlobject.SqlUpdate
import org.skife.jdbi.v2.unstable.BindIn
import org.skife.jdbi.v2.tweak.ResultSetMapper
import kotlin.Throws
import java.sql.SQLException
import java.sql.ResultSet
import org.skife.jdbi.v2.StatementContext
import org.skife.jdbi.v2.sqlobject.customizers.Mapper

@UseStringTemplate3StatementLocator
interface RepositoryDao {
    @SqlBatch("insert into repositories " +
        "(id, owner_id, name, full_name, description, fork, homepage, stargazers_count, language, created_at, updated_at, fetched_at) " +
        "values (:id, :ownerId, :name, :fullName, :description, :fork, :homepage, :stargazersCount, :language, current_timestamp(0), current_timestamp(0), current_timestamp(0)) " +
        "on conflict (id) do update set " +
        "owner_id=excluded.owner_id, name=excluded.name, full_name=excluded.full_name, description=excluded.description, homepage=excluded.homepage, stargazers_count=excluded.stargazers_count, language=excluded.language, updated_at=excluded.updated_at, fetched_at=excluded.fetched_at")
    @BatchChunkSize(100)
    fun bulkInsert(@BindBean repos: List<Repository?>?)

    @SqlQuery("select id, stargazers_count from repositories order by stargazers_count desc, id desc limit :limit")
    @Mapper(RepositoryStarMapper::class)
    fun starsDescFirstRepos(@Bind("limit") limit: Int?): List<Repository>

    @SqlQuery("select id, stargazers_count from repositories where (stargazers_count, id) \\< (:stargazersCount, :id) " +
        "order by stargazers_count desc, id desc limit :limit")
    @Mapper(RepositoryStarMapper::class)
    fun starsDescReposAfter(@Bind("stargazersCount") stargazersCount: Int?, @Bind("id") id: Long?, @Bind("limit") limit: Int?): List<Repository>

    @SqlQuery("select count(1) from repositories")
    fun countRepos(): Int

    @SqlQuery("select count(1) from repositories where stargazers_count = :stargazersCount")
    fun countReposHavingStars(@Bind("stargazersCount") stargazersCount: Int): Int

    @SqlUpdate("delete from repositories where owner_id = :userId")
    fun deleteAllOwnedBy(@Bind("userId") userId: Long?): Long

    @SqlUpdate("delete from repositories where owner_id = :userId and id not in (<ids>)")
    fun deleteAllOwnedByExcept(@Bind("userId") userId: Long?, @BindIn("ids") ids: List<Long?>?): Long
    class RepositoryStarMapper : ResultSetMapper<Repository> {
        @Throws(SQLException::class)
        override fun map(index: Int, r: ResultSet, ctx: StatementContext): Repository {
            return Repository(r.getLong("id"), r.getInt("stargazers_count"))
        }
    }
}

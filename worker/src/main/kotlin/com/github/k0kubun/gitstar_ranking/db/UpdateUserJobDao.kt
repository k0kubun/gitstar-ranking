package com.github.k0kubun.gitstar_ranking.db

import org.skife.jdbi.v2.sqlobject.SqlUpdate
import org.skife.jdbi.v2.sqlobject.Bind
import org.skife.jdbi.v2.sqlobject.SqlQuery
import com.github.k0kubun.gitstar_ranking.core.UpdateUserJob
import org.skife.jdbi.v2.tweak.ResultSetMapper
import kotlin.Throws
import java.sql.SQLException
import java.sql.ResultSet
import org.skife.jdbi.v2.StatementContext
import java.io.StringReader
import java.sql.Timestamp
import javax.json.Json
import org.skife.jdbi.v2.sqlobject.customizers.Mapper

interface UpdateUserJobDao {
    // Exclusively lock job record to dequeue.
    @SqlUpdate("with t1 as (select id from update_user_jobs where timeout_at < current_timestamp(0) order by timeout_at asc limit 1) " +
        "update update_user_jobs t2 set timeout_at = :timeoutAt, owner = pg_backend_pid() from t1 where t1.id = t2.id")
    fun acquireUntil(@Bind("timeoutAt") timeoutAt: Timestamp?): Long

    // Using the timeout value and connection_id as key, fetch payload of acquired job.
    @SqlQuery("select id, payload from update_user_jobs where timeout_at = :timeoutAt and owner = pg_backend_pid()")
    @Mapper(UpdateUserJobMapper::class)
    fun fetchByTimeout(@Bind("timeoutAt") timeoutAt: Timestamp?): UpdateUserJob?

    @SqlUpdate("delete from update_user_jobs where id = :id")
    fun delete(@Bind("id") id: Int?): Long
    class UpdateUserJobMapper : ResultSetMapper<UpdateUserJob> {
        override fun map(index: Int, r: ResultSet, ctx: StatementContext): UpdateUserJob {
            // Payload is built in: app/models/update_user_job.rb
            val payload = Json.createReader(StringReader(r.getString("payload"))).readObject()
            val userId = java.lang.Long.valueOf(payload.getInt("user_id", 0).toLong())
            return UpdateUserJob(
                id = r.getInt("id"),
                userId = if (userId != 0L) userId else null,
                userName = payload.getString("user_name", null),
                tokenUserId = java.lang.Long.valueOf(payload.getInt("token_user_id").toLong()),
            )
        }
    }
}

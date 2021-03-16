package com.github.k0kubun.gitstar_ranking.db

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.k0kubun.gitstar_ranking.core.UserUpdateJob
import com.github.k0kubun.gitstar_ranking.core.objectMapper
import java.sql.Timestamp
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.RecordMapper
import org.jooq.impl.DSL.field
import org.jooq.impl.DSL.now
import org.jooq.impl.DSL.table

private data class UserUpdateJobPayload(
    val userId: Long?,
    val userName: String? = null,
    val tokenUserId: Long,
)

class UserUpdateJobQuery(private val database: DSLContext) {
    private val userUpdateJobColumns = listOf(field("id"), field("payload"))
    private val userUpdateJobMapper = RecordMapper<Record, UserUpdateJob> { record ->
        val payload = objectMapper.readValue<UserUpdateJobPayload>(record.get("payload", String::class.java))
        UserUpdateJob(
            id = record.get("id", Long::class.java),
            userId = payload.userId,
            userName = payload.userName,
            tokenUserId = payload.tokenUserId,
        )
    }

    fun create(userId: Long, tokenUserId: Long) {
        val payload = UserUpdateJobPayload(userId = userId, tokenUserId = tokenUserId)
        database
            .insertInto(table("user_update_jobs"))
            .set(field("created_at"), now())
            .set(field("updated_at"), now())
            .set(field("timeout_at"), now())
            .set(field("payload"), objectMapper.writeValueAsString(payload))
            .execute()
    }

    fun find(timeoutAt: Timestamp): UserUpdateJob? {
        return database
            .select(userUpdateJobColumns)
            .from("user_update_jobs")
            .where(field("timeout_at").eq(timeoutAt))
            .and("owner = pg_backend_pid()")
            .fetchOne(userUpdateJobMapper)
    }

    fun delete(id: Long) {
        database
            .delete(table("user_update_jobs"))
            .where(field("id").eq(id))
            .execute()
    }

    fun acquireUntil(timeoutAt: Timestamp): Long {
        return database
            .fetchOne("""
                with t1 as (select id from user_update_jobs where timeout_at < current_timestamp(0) order by timeout_at asc limit 1) 
                update user_update_jobs t2 set timeout_at = ?, owner = pg_backend_pid() from t1 where t1.id = t2.id
            """.trimIndent(), timeoutAt)
            ?.get(0, Long::class.java) ?: 0
    }
}

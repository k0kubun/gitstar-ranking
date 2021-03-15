package com.github.k0kubun.gitstar_ranking.db

import com.github.k0kubun.gitstar_ranking.core.StarsCursor
import com.github.k0kubun.gitstar_ranking.core.User
import java.sql.Timestamp
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.RecordMapper
import org.jooq.impl.DSL.field
import org.jooq.impl.DSL.row

class UserQuery(private val database: DSLContext) {
    private val userColumns = listOf(
        "id",
        "type",
        "login",
        "stargazers_count",
        "updated_at",
    ).map { field(it) }

    private val userMapper = RecordMapper<Record, User> { record ->
        User(
            id = record.get("id", Long::class.java),
            type = record.get("type", String::class.java),
            login = record.get("login", String::class.java),
            stargazersCount = record.get("stargazers_count", Long::class.java),
            updatedAt = record.get("updated_at", Timestamp::class.java),
        )
    }

    fun count(stars: Long? = null): Long {
        return database
            .selectCount()
            .from("users")
            .where(field("type").eq("User"))
            .run {
                if (stars != null) {
                    and(field("stargazers_count", Long::class.java)!!.eq(stars))
                } else this
            }
            .fetchOne(0, Long::class.java)!!
    }

    fun orderByStarsDesc(limit: Int, after: StarsCursor? = null): List<User> {
        return database
            .select(userColumns)
            .from("users")
            .where(field("type").eq("Type"))
            .run {
                if (after != null) {
                    and(
                        row(field("stargazers_count", Long::class.java), field("id", Long::class.java))
                        .lessThan(after.stars, after.id))
                } else this
            }
            .orderBy(field("stargazers_count").desc(), field("id").desc())
            .limit(limit)
            .fetch(userMapper)
    }
}

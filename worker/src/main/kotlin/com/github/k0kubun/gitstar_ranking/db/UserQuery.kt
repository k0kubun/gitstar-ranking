package com.github.k0kubun.gitstar_ranking.db

import com.github.k0kubun.gitstar_ranking.client.UserResponse
import com.github.k0kubun.gitstar_ranking.core.StarsCursor
import com.github.k0kubun.gitstar_ranking.core.User
import com.github.k0kubun.gitstar_ranking.core.table
import java.sql.Timestamp
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.RecordMapper
import org.jooq.impl.DSL.field
import org.jooq.impl.DSL.now
import org.jooq.impl.DSL.row

class UserQuery(private val database: DSLContext) {
    private val userColumns = listOf(
        field("id"),
        field("type"),
        field("login"),
        field("stargazers_count"),
        field("updated_at", Timestamp::class.java),
    )

    private val userMapper = RecordMapper<Record, User> { record ->
        User(
            id = record.get("id", Long::class.java),
            type = record.get("type", String::class.java),
            login = record.get("login", String::class.java),
            stargazersCount = record.get("stargazers_count", Long::class.java),
            updatedAt = record.get("updated_at", Timestamp::class.java),
        )
    }

    fun find(id: Long): User? {
        return database
            .select(userColumns)
            .from("users")
            .where(field("id").eq(id))
            .fetchOne(userMapper)
    }

    fun findBy(login: String): User? {
        return database
            .select(userColumns)
            .from("users")
            .where(field("login").eq(login))
            .fetchOne(userMapper)
    }

    fun create(user: UserResponse) {
        insertAll(listOf(user))
    }

    fun update(id: Long, login: String? = null, stargazersCount: Long? = null) {
        database
            .update(table("users"))
            .set(field("updated_at"), now())
            .run {
                if (login != null) {
                    set(field("login"), login)
                } else this
            }
            .run {
                if (stargazersCount != null) {
                    set(field("stargazers_count"), stargazersCount)
                } else this
            }
            .where(field("id").eq(id))
            .execute()
    }

    fun destroy(id: Long) {
        database
            .delete(table("users"))
            .where(field("id").eq(id))
            .execute()
    }

    fun count(stargazersCount: Long? = null): Long {
        return database
            .selectCount()
            .from("users")
            .where(field("type").eq("User"))
            .run {
                if (stargazersCount != null) {
                    and(field("stargazers_count", Long::class.java)!!.eq(stargazersCount))
                } else this
            }
            .fetchOne(0, Long::class.java)!!
    }

    fun insertAll(allUsers: List<UserResponse>) {
        allUsers.chunked(100).forEach { users ->
            database
                .insertInto(table("users", primaryKey = "id"))
                .columns(
                    field("id"),
                    field("type"),
                    field("login"),
                    field("avatar_url"),
                    field("created_at"),
                    field("updated_at"),
                )
                .let {
                    users.fold(it) { query, repo ->
                        query.values(
                            repo.id,
                            repo.type,
                            repo.login,
                            repo.avatarUrl,
                            now(), // created_at
                            now(), // updated_at
                        )
                    }
                }
                .onDuplicateKeyUpdate()
                .set(field("type", String::class.java), field("excluded.type", String::class.java))
                .set(field("login", String::class.java), field("excluded.login", String::class.java))
                .set(field("avatar_url", String::class.java), field("excluded.avatar_url", String::class.java))
                .set(field("updated_at", Timestamp::class.java), field("excluded.updated_at", Timestamp::class.java))
                .execute()
        }
    }

    fun max(column: String): Long? {
        return database
            .select(field(column))
            .from("users")
            .orderBy(field(column).desc())
            .fetchOne(column, Long::class.java)
    }

    fun findStargazersCount(stargazersCountLessThan: Long): Long? {
        return database
            .select(field("stargazers_count"))
            .from("users")
            .where(field("stargazers_count").lessThan(stargazersCountLessThan))
            .orderBy(field("stargazers_count").desc())
            .fetchOne("stargazers_count", Long::class.java)
    }

    fun orderByIdAsc(stargazersCount: Long, idAfter: Long, limit: Int): List<User> {
        return database
            .select(userColumns)
            .from("users")
            .where(field("stargazers_count").eq(stargazersCount))
            .and(field("id").greaterThan(idAfter))
            .orderBy(field("id").asc())
            .limit(limit)
            .fetch(userMapper)
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

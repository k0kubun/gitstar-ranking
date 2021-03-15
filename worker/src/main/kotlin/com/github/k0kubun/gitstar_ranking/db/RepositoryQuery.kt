package com.github.k0kubun.gitstar_ranking.db

import com.github.k0kubun.gitstar_ranking.core.Repository
import com.github.k0kubun.gitstar_ranking.core.StarsCursor
import com.github.k0kubun.gitstar_ranking.core.table
import java.sql.Timestamp
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.RecordMapper
import org.jooq.impl.DSL.field
import org.jooq.impl.DSL.now
import org.jooq.impl.DSL.row

class RepositoryQuery(private val database: DSLContext) {
    private val repositoryColumns = listOf(
        "id",
        "full_name",
        "stargazers_count",
    ).map { field(it) }

    private val repositoryMapper = RecordMapper<Record, Repository> { record ->
        Repository(
            id = record.get("id", Long::class.java),
            fullName = record.get("full_name", String::class.java),
            stargazersCount = record.get("stargazers_count", Long::class.java),
        )
    }

    fun insertAll(allRepos: List<Repository>) {
        allRepos.chunked(100).forEach { repos ->
            database
                .insertInto(table("repositories", primaryKey = "id"))
                .columns(
                    field("id"),
                    field("owner_id"),
                    field("name"),
                    field("full_name"),
                    field("description"),
                    field("fork"),
                    field("homepage"),
                    field("stargazers_count"),
                    field("language"),
                    field("created_at"),
                    field("updated_at"),
                    field("fetched_at"),
                )
                .let {
                    repos.fold(it) { query, repo ->
                        query.values(
                            repo.id,
                            repo.ownerId,
                            repo.name,
                            repo.fullName,
                            repo.description,
                            repo.fork,
                            repo.homepage,
                            repo.stargazersCount,
                            repo.language,
                            now(), // created_at
                            now(), // updated_at
                            now(), // fetched_at
                        )
                    }
                }
                .onDuplicateKeyUpdate()
                .set(field("owner_id", Long::class.java), field("excluded.owner_id", Long::class.java))
                .set(field("name", String::class.java), field("excluded.name", String::class.java))
                .set(field("full_name", String::class.java), field("excluded.full_name", String::class.java))
                .set(field("description", String::class.java), field("excluded.description", String::class.java))
                .set(field("fork", Boolean::class.java), field("excluded.fork", Boolean::class.java))
                .set(field("homepage", String::class.java), field("excluded.homepage", String::class.java))
                .set(field("stargazers_count", Long::class.java), field("excluded.stargazers_count", Long::class.java))
                .set(field("language", String::class.java), field("excluded.language", String::class.java))
                .set(field("updated_at", Timestamp::class.java), field("excluded.updated_at", Timestamp::class.java))
                .set(field("fetched_at", Timestamp::class.java), field("excluded.fetched_at", Timestamp::class.java))
                .execute()
        }
    }

    fun deleteAll(ownerId: Long) {
        database
            .delete(table("repositories"))
            .where(field("owner_id").eq(ownerId))
            .execute()
    }

    fun count(stars: Long? = null): Long {
        return database
            .selectCount()
            .from("repositories")
            .run {
                if (stars != null) {
                    where(field("stargazers_count", Long::class.java)!!.eq(stars))
                } else this
            }
            .fetchOne(0, Long::class.java)!!
    }

    fun orderByStarsDesc(limit: Int, after: StarsCursor? = null): List<Repository> {
        return database
            .select(repositoryColumns)
            .from("repositories")
            .run {
                if (after != null) {
                    where(
                        row(field("stargazers_count", Long::class.java), field("id", Long::class.java))
                        .lessThan(after.stars, after.id))
                } else this
            }
            .orderBy(field("stargazers_count").desc(), field("id").desc())
            .limit(limit)
            .fetch(repositoryMapper)
    }
}

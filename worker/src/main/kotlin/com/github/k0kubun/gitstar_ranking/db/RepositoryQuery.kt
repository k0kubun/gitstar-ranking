package com.github.k0kubun.gitstar_ranking.db

import com.github.k0kubun.gitstar_ranking.core.Repository
import com.github.k0kubun.gitstar_ranking.core.StarsCursor
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.RecordMapper
import org.jooq.impl.DSL.field
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

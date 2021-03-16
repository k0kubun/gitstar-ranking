package com.github.k0kubun.gitstar_ranking.db

import com.github.k0kubun.gitstar_ranking.core.Organization
import com.github.k0kubun.gitstar_ranking.core.StarsCursor
import java.sql.Timestamp
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.RecordMapper
import org.jooq.impl.DSL.field

class OrganizationQuery(private val database: DSLContext) {
    private val organizationColumns = listOf(
        field("id"),
        field("login"),
        field("stargazers_count"),
        field("updated_at", Timestamp::class.java),
    )

    private val organizationMapper = RecordMapper<Record, Organization> { record ->
        Organization(
            id = record.get("id", Long::class.java),
            login = record.get("login", String::class.java),
            stargazersCount = record.get("stargazers_count", Long::class.java),
            updatedAt = record.get("updated_at", Timestamp::class.java),
        )
    }

    fun count(stars: Long? = null): Long {
        return database
            .selectCount()
            .from("users")
            .where(field("type").eq("Organization"))
            .run {
                if (stars != null) {
                    and("stargazers_count = ?", stars)
                } else this
            }
            .fetchOne(0, Long::class.java)!!
    }

    fun orderByStarsDesc(limit: Int, after: StarsCursor? = null): List<Organization> {
        return database
            .select(organizationColumns)
            .from("users")
            .where(field("type").eq("Organization"))
            .run {
                if (after != null) {
                    and("(stargazers_count, id) < (?, ?)", after.stars, after.id)
                } else this
            }
            .orderBy(field("stargazers_count").desc(), field("id").desc())
            .limit(limit)
            .fetch(organizationMapper)
    }
}

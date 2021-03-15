package com.github.k0kubun.gitstar_ranking.db

import com.github.k0kubun.gitstar_ranking.core.Organization
import java.sql.Timestamp
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.RecordMapper
import org.jooq.impl.DSL.field
import org.jooq.impl.DSL.row

data class RepositoryCursor(val id: Long, val stars: Long)

class OrganizationQuery(private val database: DSLContext) {
    private val organizationColumns = listOf(
        "id",
        "login",
        "stargazers_count",
        "updated_at",
    ).map { field(it) }

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
            .from("organizations")
            .where(field("type").eq("Organization"))
            .run {
                if (stars != null) {
                    and(field("stargazers_count", Long::class.java)!!.eq(stars))
                } else this
            }
            .fetchOne(0, Long::class.java)!!
    }

    fun orderByStarsDesc(limit: Int, after: RepositoryCursor? = null): List<Organization> {
        return database
            .select(organizationColumns)
            .from("organizations")
            .where(field("type").eq("Organization"))
            .run {
                if (after != null) {
                    and(row(field("stargazers_count", Long::class.java), field("id", Long::class.java))
                        .lessThan(after.stars, after.id))
                } else this
            }
            .orderBy(field("stargazers_count").desc(), field("id").desc())
            .limit(limit)
            .fetch(organizationMapper)
    }
}

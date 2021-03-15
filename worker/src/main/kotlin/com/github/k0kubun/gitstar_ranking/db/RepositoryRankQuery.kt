package com.github.k0kubun.gitstar_ranking.db

import com.github.k0kubun.gitstar_ranking.core.Organization
import com.github.k0kubun.gitstar_ranking.core.Repository
import com.github.k0kubun.gitstar_ranking.core.RepositoryRank
import com.github.k0kubun.gitstar_ranking.core.UserRank
import java.sql.Timestamp
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.RecordMapper
import org.jooq.impl.DSL
import org.jooq.impl.DSL.field
import org.jooq.impl.DSL.now
import org.jooq.impl.DSL.table

class RepositoryRankQuery(private val database: DSLContext) {
    // ranks.size is smaller than 5000 because of PaginatedRepositories
    fun insertAll(ranks: List<RepositoryRank>) {
        database
            .insertInto(table("repository_ranks"))
            .columns(field("stargazers_count"), field("rank"), field("created_at"), field("updated_at"))
            .let {
                ranks.fold(it) { query, rank ->
                    query.values(rank.stargazersCount, rank.rank, now(), now())
                }
            }
            .execute()
    }

    fun deleteByStars(min: Long, max: Long) {
        database
            .delete(table("repository_ranks"))
            .where(field("stargazers_count").between(min, max))
            .execute()
    }

    fun deleteByRank(min: Long, max: Long) {
        database
            .delete(table("repository_ranks"))
            .where(field("rank").between(min, max))
            .execute()
    }
}

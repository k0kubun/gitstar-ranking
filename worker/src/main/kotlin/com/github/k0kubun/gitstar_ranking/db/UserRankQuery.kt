package com.github.k0kubun.gitstar_ranking.db

import com.github.k0kubun.gitstar_ranking.core.UserRank
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.jooq.impl.DSL.field
import org.jooq.impl.DSL.table

class UserRankQuery(private val database: DSLContext) {
    // ranks.size is smaller than 5000 because of PaginatedUsers
    fun insertAll(ranks: List<UserRank>) {
        database
            .insertInto(table("user_ranks"))
            .columns(field("stargazers_count"), field("rank"), field("created_at"), field("updated_at"))
            .let {
                ranks.fold(it) { query, rank ->
                    query.values(rank.stargazersCount, rank.rank, DSL.now(), DSL.now())
                }
            }
            .execute()
    }

    fun deleteByStars(min: Long, max: Long) {
        database
            .delete(table("user_ranks"))
            .where(field("stargazers_count").between(min, max))
            .execute()
    }

    fun deleteByRank(min: Long, max: Long) {
        database
            .delete(table("user_ranks"))
            .where(field("rank").between(min, max))
            .execute()
    }
}

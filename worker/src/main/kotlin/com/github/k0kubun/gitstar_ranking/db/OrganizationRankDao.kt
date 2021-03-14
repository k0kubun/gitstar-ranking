package com.github.k0kubun.gitstar_ranking.db

import org.skife.jdbi.v2.sqlobject.SqlUpdate
import org.skife.jdbi.v2.sqlobject.Bind
import org.skife.jdbi.v2.sqlobject.SqlBatch
import org.skife.jdbi.v2.sqlobject.customizers.BatchChunkSize
import org.skife.jdbi.v2.sqlobject.BindBean
import com.github.k0kubun.gitstar_ranking.core.OrganizationRank

interface OrganizationRankDao {
    @SqlUpdate("delete from organization_ranks where stargazers_count between :min and :max")
    fun deleteStarsBetween(@Bind("min") min: Int, @Bind("max") max: Int): Long

    // Lower rank is larger number
    @SqlUpdate("delete from organization_ranks where rank between :highest and :lowest")
    fun deleteRankBetween(@Bind("highest") highest: Int, @Bind("lowest") lowest: Int): Long

    @SqlBatch("insert into organization_ranks (stargazers_count, rank, created_at, updated_at) " +
        "values (:stargazersCount, :rank, current_timestamp(0), current_timestamp(0))")
    @BatchChunkSize(5000)
    fun bulkInsert(@BindBean orgRanks: List<OrganizationRank?>?)
}

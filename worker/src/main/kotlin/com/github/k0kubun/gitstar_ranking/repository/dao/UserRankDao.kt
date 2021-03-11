package com.github.k0kubun.gitstar_ranking.repository.dao

import org.skife.jdbi.v2.sqlobject.SqlUpdate
import org.skife.jdbi.v2.sqlobject.Bind
import org.skife.jdbi.v2.sqlobject.SqlBatch
import org.skife.jdbi.v2.sqlobject.customizers.BatchChunkSize
import org.skife.jdbi.v2.sqlobject.BindBean
import com.github.k0kubun.gitstar_ranking.model.UserRank

interface UserRankDao {
    @SqlUpdate("delete from user_ranks where stargazers_count between :min and :max")
    fun deleteStarsBetween(@Bind("min") min: Int, @Bind("max") max: Int): Long

    // Lower rank is larger number
    @SqlUpdate("delete from user_ranks where rank between :highest and :lowest")
    fun deleteRankBetween(@Bind("highest") highest: Int, @Bind("lowest") lowest: Int): Long

    @SqlBatch("insert into user_ranks (stargazers_count, rank, created_at, updated_at) " +
        "values (:stargazersCount, :rank, current_timestamp(0), current_timestamp(0))")
    @BatchChunkSize(5000)
    fun bulkInsert(@BindBean userRanks: List<UserRank?>?)
}

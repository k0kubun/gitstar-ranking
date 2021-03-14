package com.github.k0kubun.gitstar_ranking.db

import com.github.k0kubun.gitstar_ranking.core.UserRank
import org.skife.jdbi.v2.sqlobject.Bind
import org.skife.jdbi.v2.sqlobject.BindBean
import org.skife.jdbi.v2.sqlobject.SqlBatch
import org.skife.jdbi.v2.sqlobject.SqlUpdate
import org.skife.jdbi.v2.sqlobject.customizers.BatchChunkSize

interface UserRankDao {
    @SqlUpdate("delete from user_ranks where stargazers_count between :min and :max")
    fun deleteStarsBetween(@Bind("min") min: Long, @Bind("max") max: Long): Long

    // Lower rank is larger number
    @SqlUpdate("delete from user_ranks where rank between :highest and :lowest")
    fun deleteRankBetween(@Bind("highest") highest: Long, @Bind("lowest") lowest: Long): Long

    @SqlBatch("insert into user_ranks (stargazers_count, rank, created_at, updated_at) " +
        "values (:stargazersCount, :rank, current_timestamp(0), current_timestamp(0))")
    @BatchChunkSize(5000)
    fun bulkInsert(@BindBean userRanks: List<UserRank?>?)
}

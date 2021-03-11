package com.github.k0kubun.gitstar_ranking.repository.dao;

import com.github.k0kubun.gitstar_ranking.model.UserRank;

import java.util.List;

import org.skife.jdbi.v2.sqlobject.*;
import org.skife.jdbi.v2.sqlobject.customizers.BatchChunkSize;

public interface UserRankDao
{
    @SqlUpdate("delete from user_ranks where stargazers_count between :min and :max")
    long deleteStarsBetween(@Bind("min") int min, @Bind("max") int max);

    // Lower rank is larger number
    @SqlUpdate("delete from user_ranks where rank between :highest and :lowest")
    long deleteRankBetween(@Bind("highest") int highest, @Bind("lowest") int lowest);

    @SqlBatch("insert into user_ranks (stargazers_count, rank, created_at, updated_at) " +
            "values (:stargazersCount, :rank, current_timestamp(0), current_timestamp(0))")
    @BatchChunkSize(5000)
    void bulkInsert(@BindBean List<UserRank> userRanks);
}

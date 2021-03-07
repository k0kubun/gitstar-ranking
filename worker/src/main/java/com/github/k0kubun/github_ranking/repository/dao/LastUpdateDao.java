package com.github.k0kubun.github_ranking.repository.dao;

import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;

public interface LastUpdateDao {
    public static final int USER_FULL_SCAN = 1;

    @SqlQuery("select user_id from last_updates where id = :key")
    long userId(@Bind("key") int key);

    @SqlUpdate("insert into last_updates (id, user_id, updated_at) " +
            "values (:key, :user_id, current_timestamp(0)) on conflict (id) do update set " +
            "user_id=excluded.user_id, updated_at=excluded.updated_at")
    void updateUserId(@Bind("key") int key, @Bind("user_id") long userId);
}

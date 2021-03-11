package com.github.k0kubun.gitstar_ranking.repository.dao;

import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;

public interface LastUpdateDao {
    int FULL_SCAN_USER_ID = 1;
    int STAR_SCAN_USER_ID = 2;
    int STAR_SCAN_STARS = 3;

    @SqlQuery("select cursor from last_updates where id = :key")
    long getCursor(@Bind("key") int key);

    @SqlUpdate("insert into last_updates (id, cursor, updated_at) " +
            "values (:key, :cursor, current_timestamp(0)) on conflict (id) do update set " +
            "cursor=excluded.cursor, updated_at=excluded.updated_at")
    void updateCursor(@Bind("key") int key, @Bind("cursor") long cursor);

    @SqlQuery("delete from last_updates where id = :key")
    long resetCursor(@Bind("key") int key);
}

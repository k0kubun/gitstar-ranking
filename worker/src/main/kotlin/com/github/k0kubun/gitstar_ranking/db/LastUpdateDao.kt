package com.github.k0kubun.gitstar_ranking.db

import org.skife.jdbi.v2.sqlobject.SqlQuery
import org.skife.jdbi.v2.sqlobject.Bind
import org.skife.jdbi.v2.sqlobject.SqlUpdate

const val FULL_SCAN_USER_ID = 1
const val STAR_SCAN_USER_ID = 2
const val STAR_SCAN_STARS = 3

interface LastUpdateDao {
    @SqlQuery("select cursor from last_updates where id = :key")
    fun getCursor(@Bind("key") key: Int): Long

    @SqlUpdate("insert into last_updates (id, cursor, updated_at) " +
        "values (:key, :cursor, current_timestamp(0)) on conflict (id) do update set " +
        "cursor=excluded.cursor, updated_at=excluded.updated_at")
    fun updateCursor(@Bind("key") key: Int, @Bind("cursor") cursor: Long)

    @SqlQuery("delete from last_updates where id = :key")
    fun resetCursor(@Bind("key") key: Int): Long
}

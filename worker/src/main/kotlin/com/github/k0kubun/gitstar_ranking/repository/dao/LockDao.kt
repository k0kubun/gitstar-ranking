package com.github.k0kubun.gitstar_ranking.repository.dao

import org.skife.jdbi.v2.sqlobject.SqlUpdate
import org.skife.jdbi.v2.sqlobject.Bind

interface LockDao {
    @SqlUpdate("select pg_advisory_xact_lock(:key)")
    fun getLock(@Bind("key") key: Long)
}

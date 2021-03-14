package com.github.k0kubun.gitstar_ranking.db

import org.skife.jdbi.v2.sqlobject.SqlQuery
import org.skife.jdbi.v2.sqlobject.Bind

interface AccessTokenDao {
    @SqlQuery("select token from access_tokens where user_id = :userId")
    fun findByUserId(@Bind("userId") userId: Long): String?
}

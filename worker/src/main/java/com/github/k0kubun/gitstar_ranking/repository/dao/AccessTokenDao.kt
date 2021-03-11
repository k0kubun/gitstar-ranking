package com.github.k0kubun.gitstar_ranking.repository.dao

import org.skife.jdbi.v2.sqlobject.SqlQuery
import org.skife.jdbi.v2.sqlobject.Bind
import com.github.k0kubun.gitstar_ranking.model.AccessToken
import org.skife.jdbi.v2.tweak.ResultSetMapper
import kotlin.Throws
import java.sql.SQLException
import java.sql.ResultSet
import org.skife.jdbi.v2.StatementContext
import org.skife.jdbi.v2.sqlobject.customizers.Mapper

interface AccessTokenDao {
    @SqlQuery("select token from access_tokens where user_id = :userId")
    @Mapper(AccessTokenMapper::class)
    fun findByUserId(@Bind("userId") userId: Long?): AccessToken?

    class AccessTokenMapper : ResultSetMapper<AccessToken> {
        @Throws(SQLException::class)
        override fun map(index: Int, r: ResultSet, ctx: StatementContext): AccessToken {
            return AccessToken(
                r.getString("token")
            )
        }
    }
}

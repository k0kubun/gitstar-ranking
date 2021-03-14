package com.github.k0kubun.gitstar_ranking.db

import org.jooq.DSLContext
import org.jooq.impl.DSL.field

class AccessTokenQuery(private val database: DSLContext) {
    fun findToken(userId: Long): String? {
        return database
            .select(field("token"))
            .from("access_tokens")
            .where(field("user_id").eq(userId))
            .fetchOne()
            ?.get("token", String::class.java)
    }
}

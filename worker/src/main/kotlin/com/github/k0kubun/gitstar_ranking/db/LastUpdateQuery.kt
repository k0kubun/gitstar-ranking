package com.github.k0kubun.gitstar_ranking.db

import org.jooq.DSLContext
import org.jooq.impl.DSL.field
import org.jooq.impl.DSL.now
import org.jooq.impl.DSL.table

const val FULL_SCAN_USER_ID = 1
const val STAR_SCAN_USER_ID = 2
const val STAR_SCAN_STARS = 3

class LastUpdateQuery(private val database: DSLContext) {
    fun findCursor(key: Int): Long? {
        return database
            .select(field("cursor"))
            .from("last_updates")
            .where(field("id").eq(key))
            .fetchOne()
            ?.get("cursor", Long::class.java)
    }

    fun update(key: Int, cursor: Long) {
        database
            .insertInto(table("last_updates"))
            .set(field("key"), key)
            .set(field("cursor"), cursor)
            .set(field("updated_at"), now())
            .onDuplicateKeyUpdate()
            .set(field("cursor"), table("excluded").field("cursor"))
            .set(field("updated_at"), table("excluded").field("updated_at"))
            .execute()
    }

    fun delete(key: List<Int>) {
        database
            .delete(table("last_updates"))
            .where(field("id").`in`(*key.toTypedArray()))
            .execute()
    }
}

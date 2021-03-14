package com.github.k0kubun.gitstar_ranking.db

import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.jooq.impl.DSL.field

private const val SHARED_KEY: Long = 0
private const val UPDATE_USER_JOBS_LOCK = 0.toChar()
private const val USER_UPDATE_LOCK = 1.toChar()

class DatabaseLock(private val database: DSLContext) {
    fun withUserUpdate(userId: Long, callback: () -> Unit) {
        database.transaction { tx ->
            DSL.using(tx).getLock(key = userId, namespace = USER_UPDATE_LOCK)
            callback()
        }
    }

    // Lock for `acquireUntil`. We need this to execute `acquireUntil` because concurrent execution of the query causes dead lock...:
    // com.mysql.cj.jdbc.exceptions.MySQLTransactionRollbackException: Deadlock found when trying to get lock; try restarting transaction
    fun withUpdateUserJobs(callback: () -> Long): Long {
        return database.transactionResult { tx ->
            DSL.using(tx).getLock(key = SHARED_KEY, namespace = UPDATE_USER_JOBS_LOCK)
            callback()
        }
    }

    private fun DSLContext.getLock(key: Long, namespace: Char) {
        // `pg_advisory_xact_lock(key1 int, key2 int)` is int/int, so it's not useful
        val lockKey = (key shl 8) + namespace.toLong()
        select(field("pg_advisory_xact_lock($lockKey)")).execute()
    }
}

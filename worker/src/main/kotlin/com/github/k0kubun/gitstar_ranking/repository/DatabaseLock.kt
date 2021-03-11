package com.github.k0kubun.gitstar_ranking.repository

import org.skife.jdbi.v2.TransactionStatus
import com.github.k0kubun.gitstar_ranking.repository.dao.LockDao
import kotlin.Throws
import java.io.IOException
import org.skife.jdbi.v2.Handle

class DatabaseLock(lockHandle: Handle) {
    private val lockHandle: Handle
    fun withUserUpdate(userId: Long, callback: UserUpdateCallback) {
        lockHandle.useTransaction { conn: Handle, _: TransactionStatus? ->
            val dao = conn.attach(LockDao::class.java)
            getLock(dao, userId, USER_UPDATE_LOCK)
            callback.withLock()
        }
    }

    // Lock for `acquireUntil`. We need this to execute `acquireUntil` because concurrent execution of the query causes dead lock...:
    // com.mysql.cj.jdbc.exceptions.MySQLTransactionRollbackException: Deadlock found when trying to get lock; try restarting transaction
    fun withUpdateUserJobs(callback: UpdateUserJobCallback): Long {
        return lockHandle.inTransaction { conn: Handle, _: TransactionStatus? ->
            val dao = conn.attach(LockDao::class.java)
            getLock(dao, SHARED_KEY, UPDATE_USER_JOBS_LOCK)
            callback.withLock()
        }
    }

    private fun getLock(dao: LockDao, key: Long, namespace: Char) {
        // `pg_advisory_xact_lock(key1 int, key2 int)` is int/int, so not useful
        dao.getLock((key shl 8) + namespace.toLong())
    }

    fun interface UserUpdateCallback {
        @Throws(IOException::class)
        fun withLock()
    }

    fun interface UpdateUserJobCallback {
        fun withLock(): Long
    }

    companion object {
        private const val SHARED_KEY: Long = 0
        private const val UPDATE_USER_JOBS_LOCK = 0.toChar()
        private const val USER_UPDATE_LOCK = 1.toChar()
    }

    init {
        lockHandle.connection.autoCommit = false
        this.lockHandle = lockHandle
    }
}

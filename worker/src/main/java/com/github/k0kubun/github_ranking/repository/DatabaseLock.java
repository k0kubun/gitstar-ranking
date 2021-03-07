package com.github.k0kubun.github_ranking.repository;

import com.github.k0kubun.github_ranking.repository.dao.LockDao;
import com.github.k0kubun.github_ranking.repository.dao.UpdateUserJobDao;
import com.github.k0kubun.github_ranking.worker.Worker;

import java.io.IOException;
import java.sql.SQLException;

import org.skife.jdbi.v2.Handle;

public class DatabaseLock
{
    private static final long SHARED_KEY = 0;
    private static final char UPDATE_USER_JOBS_LOCK = 0;
    private static final char USER_UPDATE_LOCK = 1;

    private final Handle lockHandle;

    public DatabaseLock(Handle lockHandle) throws SQLException
    {
        lockHandle.getConnection().setAutoCommit(false);
        this.lockHandle = lockHandle;
    }

    public void withUserUpdate(Long userId, UserUpdateCallback callback)
    {
        lockHandle.useTransaction((conn, status) -> {
            LockDao dao = conn.attach(LockDao.class);
            getLock(dao, userId, USER_UPDATE_LOCK);
            callback.withLock();
        });
    }

    // Lock for `acquireUntil`. We need this to execute `acquireUntil` because concurrent execution of the query causes dead lock...:
    // com.mysql.cj.jdbc.exceptions.MySQLTransactionRollbackException: Deadlock found when trying to get lock; try restarting transaction
    public long withUpdateUserJobs(UpdateUserJobCallback callback)
    {
        return lockHandle.inTransaction((conn, status) -> {
            LockDao dao = conn.attach(LockDao.class);
            getLock(dao, SHARED_KEY, UPDATE_USER_JOBS_LOCK);
            return callback.withLock(conn.attach(UpdateUserJobDao.class));
        });
    }

    private void getLock(LockDao dao, long key, char namespace) {
        // `pg_advisory_xact_lock(key1 int, key2 int)` is int/int, so not useful
        dao.getLock((key << 8) + namespace);
    }

    @FunctionalInterface
    public interface UserUpdateCallback
    {
        void withLock() throws IOException;
    }

    @FunctionalInterface
    public interface UpdateUserJobCallback
    {
        long withLock(UpdateUserJobDao dao);
    }
}

package com.github.k0kubun.github_ranking.repository;

import com.github.k0kubun.github_ranking.repository.dao.LockDao;
import com.github.k0kubun.github_ranking.repository.dao.UpdateUserJobDao;
import com.github.k0kubun.github_ranking.worker.Worker;

import java.io.IOException;

import org.skife.jdbi.v2.Handle;

public class DatabaseLock
{
    private static final String UPDATE_USER_JOBS_LOCK = "update_user_jobs";
    private static final String USER_UPDATE_LOCK_PREFIX = "user_update:";

    private final LockDao dao;
    private final Handle handle;
    private final Worker worker;

    public DatabaseLock(Handle handle, Worker worker)
    {
        dao = handle.attach(LockDao.class);
        this.handle = handle;
        this.worker = worker;
    }

    public void withUserUpdate(Integer userId, UserUpdateCallback callback)
            throws IOException
    {
        boolean locked = false;

        try {
            while (!(locked = (dao.getLock(USER_UPDATE_LOCK_PREFIX + userId.toString(), 10) == 1))) {
                if (worker.isStopped) {
                    return;
                }
            }
            callback.withLock();
        }
        finally {
            if (locked) {
                dao.releaseLock(USER_UPDATE_LOCK_PREFIX + userId.toString());
            }
        }
    }

    // Lock for `acquireUntil`. We need this to execute `acquireUntil` because concurrent execution of the query causes dead lock...:
    // com.mysql.cj.jdbc.exceptions.MySQLTransactionRollbackException: Deadlock found when trying to get lock; try restarting transaction
    public long withUpdateUserJobs(UpdateUserJobCallback callback)
    {
        boolean locked = false;

        try {
            while (!(locked = (dao.getLock(UPDATE_USER_JOBS_LOCK, 10) == 1))) {
                if (worker.isStopped) {
                    return 0;
                }
            }
            return callback.withLock(handle.attach(UpdateUserJobDao.class));
        }
        finally {
            if (locked) {
                dao.releaseLock(UPDATE_USER_JOBS_LOCK);
            }
        }
    }

    @FunctionalInterface
    public interface UserUpdateCallback
    {
        void withLock()
                throws IOException;
    }

    @FunctionalInterface
    public interface UpdateUserJobCallback
    {
        long withLock(UpdateUserJobDao dao);
    }
}

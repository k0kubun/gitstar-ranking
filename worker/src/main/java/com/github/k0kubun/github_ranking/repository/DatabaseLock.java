package com.github.k0kubun.github_ranking.repository;

import com.github.k0kubun.github_ranking.repository.dao.UpdateUserJobDao;
import com.github.k0kubun.github_ranking.repository.dao.LockDao;
import com.github.k0kubun.github_ranking.worker.Worker;
import org.skife.jdbi.v2.Handle;

public class DatabaseLock
{
    private final LockDao dao;
    private final Handle handle;
    private final Worker worker;

    public DatabaseLock(Handle handle, Worker worker)
    {
        dao = handle.attach(LockDao.class);
        this.handle = handle;
        this.worker = worker;
    }

    // Lock for `acquireUntil`. We need this to execute `acquireUntil` because concurrent execution of the query causes dead lock...:
    // com.mysql.cj.jdbc.exceptions.MySQLTransactionRollbackException: Deadlock found when trying to get lock; try restarting transaction
    public long withUpdateUserJob(UpdateUserJobCallback callback)
    {
        boolean locked = false;

        try {
            while (!(locked = (dao.getLock(10) == 1))) {
                if (worker.isStopped) {
                    return 0;
                }
            }
            return callback.withLock(handle.attach(UpdateUserJobDao.class));
        }
        finally {
            if (locked) {
                dao.releaseLock();
            }
        }
    }

    @FunctionalInterface
    public interface UserUpdateCallback
    {
        long withLock();
    }

    @FunctionalInterface
    public interface UpdateUserJobCallback
    {
        long withLock(UpdateUserJobDao dao);
    }
}

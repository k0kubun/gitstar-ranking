package com.github.k0kubun.github_ranking.repository;

import com.github.k0kubun.github_ranking.repository.dao.UpdateUserJobDao;
import com.github.k0kubun.github_ranking.worker.Worker;
import org.skife.jdbi.v2.Handle;

public class DatabaseLock
{
    private final Handle handle;
    private final Worker worker;

    public DatabaseLock(Handle handle, Worker worker)
    {
        this.handle = handle;
        this.worker = worker;
    }

    public long withUpdateUserJob(UpdateUserJobCallback callback)
    {
        UpdateUserJobDao dao = handle.attach(UpdateUserJobDao.class);
        boolean locked = false;

        try {
            while (!(locked = (dao.getLock(10) == 1))) {
                if (worker.isStopped) {
                    return 0;
                }
            }
            return callback.withLock(dao);
        }
        finally {
            if (locked) {
                dao.releaseLock();
            }
        }
    }

    @FunctionalInterface
    public interface UpdateUserJobCallback
    {
        long withLock(UpdateUserJobDao dao);
    }
}

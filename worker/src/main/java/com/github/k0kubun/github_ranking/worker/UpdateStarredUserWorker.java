package com.github.k0kubun.github_ranking.worker;

import com.github.k0kubun.github_ranking.model.UpdateUserJob;
import com.github.k0kubun.github_ranking.model.User;
import com.github.k0kubun.github_ranking.repository.DatabaseLock;
import com.github.k0kubun.github_ranking.repository.dao.UpdateUserJobDao;
import com.github.k0kubun.github_ranking.repository.dao.UserDao;

import java.sql.Timestamp;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.skife.jdbi.v2.Handle;

// This job must finish within TIMEOUT_MINUTES (1 min). Otherwise it will be infinitely retried.
public class UpdateStarredUserWorker
        extends UpdateUserWorker
{
    public UpdateStarredUserWorker(DataSource dataSource)
    {
        super(dataSource);
    }

    // Dequeue a record from update_user_jobs and call updateUser().
    @Override
    public void perform()
            throws Exception
    {
        try (Handle handle = dbi.open()) {
            //DatabaseLock lock = new DatabaseLock(handle, this);

            // Succeeded to acquire a job. Fetch job to execute.

            //try {
            //    lock.withUserUpdate(job.getUserId(), () -> {
            //        User user = handle.attach(UserDao.class).find(job.getUserId());
            //        LOG.info("started to updateUser: (userId = " + job.getUserId() + ", login = " + user.getLogin() + ")");
            //        updateUser(handle, user, job.getTokenUserId());
            //        LOG.info("finished to updateUser: (userId = " + job.getUserId() + ", login = " + user.getLogin() + ")");
            //    });
            //}
            //catch (Exception e) {
            //    LOG.error("Failed to updateUser! (userId = " + job.getUserId() + "): " + e.toString() + ": " + e.getMessage());
            //    e.printStackTrace();
            //}
        }
    }
}

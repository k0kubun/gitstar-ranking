package com.github.k0kubun.github_ranking.worker;

import com.github.k0kubun.github_ranking.config.Config;
import com.github.k0kubun.github_ranking.repository.DatabaseLock;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.skife.jdbi.v2.Handle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpdateSearchedUserWorker
        extends UpdateUserWorker
{
    private static final Logger LOG = LoggerFactory.getLogger(UpdateSearchedUserWorker.class);

    private final BlockingQueue<Boolean> searchedUserQueue;

    public UpdateSearchedUserWorker(Config config)
    {
        super(config.getDatabaseConfig().getDataSource());
        searchedUserQueue = config.getQueueConfig().getSearchedUserQueue();
    }

    @Override
    public void perform()
            throws Exception
    {
        while (searchedUserQueue.poll(5, TimeUnit.SECONDS) == null) {
            if (isStopped) {
                return;
            }
        }
        LOG.info("----- started UpdateSearchedUserWorker -----");
        try (Handle handle = dbi.open()) {
            importSearchedUsers(handle);
        }
        LOG.info("----- finished UpdateSearchedUserWorker -----");
    }

    private void importSearchedUsers(Handle handle)
            throws IOException
    {
        //DatabaseLock lock = new DatabaseLock(handle, this);

        //while (!(users = clientBuilder.buildEnabled().getUsersSince(since)).isEmpty()) {
        //    handle.attach(UserDao.class).bulkInsert(users);
        //    for (User user : users) {
        //        if (isStopped) {
        //            return;
        //        }

        //        try {
        //            lock.withUserUpdate(user.getId(), () -> {
        //                LOG.info("NewUserWorker started: (userId = " + user.getId() + ", login = " + user.getLogin() + ")");
        //                updateUser(handle, user, clientBuilder.buildEnabled());
        //                LOG.info("NewUserWorker finished: (userId = " + user.getId() + ", login = " + user.getLogin() + ")");
        //            });
        //        }
        //        catch (Exception e) {
        //            Sentry.capture(e);
        //            LOG.error("Error in NewUserWorker! (userId = " + user.getId() + "): " + e.toString() + ": " + e.getMessage());
        //        }
        //    }
        //    since = users.get(users.size() - 1).getId();

        //    // Sleep to decrease GitHub API load
        //    try {
        //        TimeUnit.SECONDS.sleep(5);
        //    }
        //    catch (InterruptedException e) {
        //        Sentry.capture(e);
        //    }
        //}
    }
}

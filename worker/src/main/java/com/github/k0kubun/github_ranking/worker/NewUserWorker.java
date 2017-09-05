package com.github.k0kubun.github_ranking.worker;

import com.github.k0kubun.github_ranking.config.Config;
import com.github.k0kubun.github_ranking.github.GitHubClientBuilder;
import com.github.k0kubun.github_ranking.model.User;
import com.github.k0kubun.github_ranking.repository.DatabaseLock;
import com.github.k0kubun.github_ranking.repository.dao.UserDao;
import io.sentry.Sentry;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NewUserWorker
        extends UpdateUserWorker
{
    private static final Logger LOG = LoggerFactory.getLogger(NewUserWorker.class);

    private final BlockingQueue<Boolean> newUserQueue;

    public NewUserWorker(Config config)
    {
        super(config.getDatabaseConfig().getDataSource());
        newUserQueue = config.getQueueConfig().getNewUserQueue();
    }

    @Override
    public void perform()
            throws Exception
    {
        while (newUserQueue.poll(5, TimeUnit.SECONDS) == null) {
            if (isStopped) {
                return;
            }
        }
        LOG.info("----- started NewUserWorker -----");
        try (Handle handle = dbi.open()) {
            importNewUsers(handle);
        }
        LOG.info("----- finished NewUserWorker -----");
    }

    private void importNewUsers(Handle handle)
            throws IOException
    {
        DatabaseLock lock = new DatabaseLock(handle, this);
        int since = handle.attach(UserDao.class).lastId();

        List<User> users;
        while (!(users = clientBuilder.buildEnabled().getUsersSince(since)).isEmpty()) {
            handle.attach(UserDao.class).bulkInsert(users);
            for (User user : users) {
                if (isStopped) {
                    return;
                }

                try {
                    lock.withUserUpdate(user.getId(), () -> {
                        LOG.info("NewUserWorker started: (userId = " + user.getId() + ", login = " + user.getLogin() + ")");
                        updateUser(handle, user, clientBuilder.buildEnabled());
                        LOG.info("NewUserWorker finished: (userId = " + user.getId() + ", login = " + user.getLogin() + ")");
                    });
                }
                catch (Exception e) {
                    Sentry.capture(e);
                    LOG.error("Error in NewUserWorker! (userId = " + user.getId() + "): " + e.toString() + ": " + e.getMessage());
                }
            }
            since = users.get(users.size() - 1).getId();

            // Sleep to decrease GitHub API load
            try {
                TimeUnit.SECONDS.sleep(5);
            }
            catch (InterruptedException e) {
                Sentry.capture(e);
            }
        }
    }
}

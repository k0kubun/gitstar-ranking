package com.github.k0kubun.github_ranking.worker;

import com.github.k0kubun.github_ranking.config.Config;
import com.github.k0kubun.github_ranking.github.GitHubClient;
import com.github.k0kubun.github_ranking.model.User;
import com.github.k0kubun.github_ranking.repository.DatabaseLock;
import com.github.k0kubun.github_ranking.repository.dao.UserDao;
import io.sentry.Sentry;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.json.JsonObject;

import org.skife.jdbi.v2.Handle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LegacyUpdateSearchedUserWorker
        extends UpdateUserWorker
{
    private static final Logger LOG = LoggerFactory.getLogger(LegacyUpdateSearchedUserWorker.class);

    private final BlockingQueue<Boolean> searchedUserQueue;

    public LegacyUpdateSearchedUserWorker(Config config)
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
        LOG.info("----- started LegacyUpdateSearchedUserWorker -----");
        try (Handle handle = dbi.open()) {
            importSearchedUsers(handle);
        }
        LOG.info("----- finished LegacyUpdateSearchedUserWorker -----");
    }

    private void importSearchedUsers(Handle handle)
            throws IOException
    {
        DatabaseLock lock = new DatabaseLock(handle, this);
        PaginatedSearchedUsers paginatedUsers = new PaginatedSearchedUsers(handle, clientBuilder.buildEnabled());

        List<User> users;
        while (!(users = paginatedUsers.getUsers()).isEmpty()) {
            List<User> absentUsers = selectUsersWithoutUpdatedAt(users); // no need to unique?
            if (!absentUsers.isEmpty()) {
                handle.attach(UserDao.class).bulkInsert(absentUsers);
            }

            for (User user : users) {
                if (isStopped) {
                    return;
                }

                if (user.getUpdatedAt() != null && user.isUpdatedWithinDays(5)) {
                    LOG.info("LegacyUpdateSearchedUserWorker skipped: (userId = " + user.getId() + ", login = " + user.getLogin() + ", updatedAt = " + user.getUpdatedAt().toString() + ")");
                    continue;
                }

                try {
                    lock.withUserUpdate(user.getId(), () -> {
                        LOG.info("LegacyUpdateSearchedUserWorker started: (userId = " + user.getId() + ", login = " + user.getLogin() + ")");
                        updateUser(handle, user, clientBuilder.buildEnabled());
                        LOG.info("LegacyUpdateSearchedUserWorker finished: (userId = " + user.getId() + ", login = " + user.getLogin() + ")");
                    });
                }
                catch (Exception e) {
                    Sentry.capture(e);
                    LOG.error("Error in LegacyUpdateSearchedUserWorker! (userId = " + user.getId() + "): " + e.toString() + ": " + e.getMessage());
                }
            }
        }
    }

    // Filter out users and return users who are absent in database.
    private List<User> selectUsersWithoutUpdatedAt(List<User> users)
    {
        List<User> result = new ArrayList<>();
        for (User user : users) {
            if (user.getUpdatedAt() == null) {
                result.add(user);
            }
        }
        return result;
    }

    public class PaginatedSearchedUsers
    {
        private final GitHubClient client;
        private final UserDao dao;
        private String cursor;

        public PaginatedSearchedUsers(Handle handle, GitHubClient client)
        {
            this.client = client;
            dao = handle.attach(UserDao.class);
            cursor = null;
        }

        // Return users with id, login, type, avatarUrl, and optional updatedAt.
        public List<User> getUsers()
                throws IOException
        {
            List<User> users = fetchUsers();
            loadUpdatedAt(users);
            return users;
        }

        // Return users with id, login, type, avatarUrl
        private List<User> fetchUsers()
                throws IOException
        {
            List<User> users = new ArrayList<>();
            List<JsonObject> userEdges = client.getStarsDescUserEdges(cursor);
            for (JsonObject userEdge : userEdges) {
                JsonObject owner = userEdge.getJsonObject("node").getJsonObject("owner");
                users.add(buildUserFromObject(owner));
            }
            cursor = userEdges.get(userEdges.size() - 1).getString("cursor");
            return users;
        }

        private User buildUserFromObject(JsonObject object)
        {
            String decodedId = new String(Base64.getDecoder().decode(object.getString("id")));
            User user;
            if (decodedId.startsWith("012:Organization")) {
                int id = Integer.valueOf(decodedId.replaceFirst("12:Organization", ""));
                user = new User(id, "Organization");
            }
            else if (decodedId.startsWith("04:User")) {
                int id = Integer.valueOf(decodedId.replaceFirst("04:User", ""));
                user = new User(id, "User");
            }
            else {
                throw new RuntimeException("unexpected decodedI: " + decodedId);
            }

            user.setLogin(object.getString("login"));
            user.setAvatarUrl(object.getString("avatarUrl"));
            return user;
        }

        // Set updatedAt to existing users. Absent users will have null updatedAt.
        private void loadUpdatedAt(List<User> users)
        {
            List<User> updatedUsers = dao.findUsersWithUpdatedAt(buildUserIds(users));
            for (User updatedUser : updatedUsers) {
                for (User user : filterUsersById(users, updatedUser.getId())) {
                    user.setUpdatedAt(updatedUser.getUpdatedAt());
                }
            }
        }

        private List<User> filterUsersById(List<User> users, long id)
        {
            List<User> result = new ArrayList<>();
            for (User user : users) {
                if (user.getId() == id) {
                    result.add(user);
                }
            }
            return result;
        }

        private List<Long> buildUserIds(List<User> users)
        {
            List<Long> ids = new ArrayList<>();
            for (User user : users) {
                ids.add(Long.valueOf(user.getId()));
            }
            return ids;
        }
    }
}

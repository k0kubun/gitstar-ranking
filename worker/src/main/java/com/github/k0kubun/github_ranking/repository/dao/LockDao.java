package com.github.k0kubun.github_ranking.repository.dao;

import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;

public interface LockDao
{
    @SqlQuery("select get_lock(:key, :timeout)")
    long getLock(@Bind("key") String key, @Bind("timeout") long timeout);

    @SqlUpdate("do release_lock(:key)")
    long releaseLock(@Bind("key") String key);
}

package com.github.k0kubun.gitstar_ranking.repository.dao;

import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;

public interface LockDao
{
    @SqlUpdate("select pg_advisory_xact_lock(:key)")
    void getLock(@Bind("key") long key);
}

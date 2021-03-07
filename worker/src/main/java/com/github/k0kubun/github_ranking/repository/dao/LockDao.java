package com.github.k0kubun.github_ranking.repository.dao;

import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;

public interface LockDao
{
    @SqlQuery("select pg_advisory_xact_lock(:key)")
    void getLock(@Bind("key") long key);
}

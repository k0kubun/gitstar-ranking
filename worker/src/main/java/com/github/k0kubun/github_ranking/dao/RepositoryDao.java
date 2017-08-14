package com.github.k0kubun.github_ranking.dao;

import com.github.k0kubun.github_ranking.model.Repository;
import java.util.List;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.BindBean;
import org.skife.jdbi.v2.sqlobject.SqlBatch;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.customizers.BatchChunkSize;
import org.skife.jdbi.v2.sqlobject.customizers.Mapper;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

public interface RepositoryDao
{
    @SqlQuery("select id, owner_id, name, full_name, description, fork, homepage, stargazers_count, language " +
              "from repositories where id = :id")
    @Mapper(RepositoryMapper.class)
    Repository find(@Bind("id") Integer id);

    @SqlBatch("insert into repositories " +
              "(id, owner_id, name, full_name, description, fork, homepage, stargazers_count, language, created_at, updated_at, fetched_at) " +
              "values (:id, :ownerId, :name, :fullName, :description, :fork, :homepage, :stargazersCount, :language, current_timestamp(), current_timestamp(), current_timestamp()) " +
              "on duplicate key update " +
              "owner_id=values(owner_id), name=values(name), full_name=values(full_name), description=values(description), homepage=values(homepage), stargazers_count=values(stargazers_count), language=values(language), updated_at=values(updated_at), fetched_at=values(fetched_at)")
    @BatchChunkSize(1000)
    void bulkInsert(@BindBean List<Repository> repos);

    class RepositoryMapper implements ResultSetMapper<Repository>
    {
        @Override
        public Repository map(int index, ResultSet r, StatementContext ctx) throws SQLException
        {
            return new Repository(
                    r.getLong("id"),
                    r.getInt("owner_id"),
                    r.getString("name"),
                    r.getString("full_name"),
                    r.getString("description"),
                    r.getBoolean("fork"),
                    r.getString("homepage"),
                    r.getInt("stargazers_count"),
                    r.getString("language")
            );
        }
    }
}

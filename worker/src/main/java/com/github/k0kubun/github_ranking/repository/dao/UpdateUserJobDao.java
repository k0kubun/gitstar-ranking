package com.github.k0kubun.github_ranking.repository.dao;

import com.github.k0kubun.github_ranking.model.UpdateUserJob;

import java.io.StringReader;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import javax.json.Json;
import javax.json.JsonObject;

import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.Mapper;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

public interface UpdateUserJobDao
{
    // Exclusively lock job record to dequeue.
    @SqlUpdate("with t1 as (select id from update_user_jobs where timeout_at < current_timestamp(0) order by timeout_at asc limit 1) " +
            "update update_user_jobs t2 set timeout_at = :timeoutAt, owner = pg_backend_pid() from t1 where t1.id = t2.id")
    long acquireUntil(@Bind("timeoutAt") Timestamp timeoutAt);

    // Using the timeout value and connection_id as key, fetch payload of acquired job.
    @SqlQuery("select id, payload from update_user_jobs where timeout_at = :timeoutAt and owner = connection_id()")
    @Mapper(UpdateUserJobMapper.class)
    UpdateUserJob fetchByTimeout(@Bind("timeoutAt") Timestamp timeoutAt);

    @SqlUpdate("delete from update_user_jobs where id = :id")
    long delete(@Bind("id") Integer id);

    class UpdateUserJobMapper
            implements ResultSetMapper<UpdateUserJob>
    {
        @Override
        public UpdateUserJob map(int index, ResultSet r, StatementContext ctx)
                throws SQLException
        {
            // Payload is built in: app/models/update_user_job.rb
            JsonObject payload = Json.createReader(new StringReader(r.getString("payload"))).readObject();
            return new UpdateUserJob(
                    r.getInt("id"),
                    Long.valueOf(payload.getInt("user_id", 0)),
                    payload.getString("user_name", null),
                    Long.valueOf(payload.getInt("token_user_id"))
            );
        }
    }
}

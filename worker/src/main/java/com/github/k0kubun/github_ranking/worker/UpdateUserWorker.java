package com.github.k0kubun.github_ranking.worker;

import com.github.k0kubun.github_ranking.config.Config;
import com.github.k0kubun.github_ranking.dao.UserDao;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.skife.jdbi.v2.DBI;

public class UpdateUserWorker extends Worker
{
    private static final Logger LOG = Worker.buildLogger(UpdateUserWorker.class.getName());
    private final DBI dbi;

    public UpdateUserWorker(Config config)
    {
        super();
        dbi = new DBI(config.getDatabaseConfig().getDataSource());
    }

    @Override
    public void perform() throws Exception
    {
        // TODO: implement
        LOG.info(dbi.onDemand(UserDao.class).find(1).getLogin());
        TimeUnit.SECONDS.sleep(1);
    }
}

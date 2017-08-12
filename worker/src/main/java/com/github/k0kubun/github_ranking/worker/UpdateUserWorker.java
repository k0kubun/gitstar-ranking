package com.github.k0kubun.github_ranking.worker;

import com.github.k0kubun.github_ranking.config.Config;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import javax.sql.DataSource;

public class UpdateUserWorker extends Worker
{
    private static final Logger LOG = Worker.buildLogger(UpdateUserWorker.class.getName());
    private final DataSource dataSource;

    public UpdateUserWorker(Config config)
    {
        super();
        dataSource = config.getDatabaseConfig().getDataSource();
    }

    @Override
    public void perform() throws Exception
    {
        // TODO: implement
        LOG.info("hello");
        TimeUnit.SECONDS.sleep(1);
    }
}

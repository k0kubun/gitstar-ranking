package com.github.k0kubun.github_ranking.worker;

import com.github.k0kubun.github_ranking.config.Config;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class UpdateUserWorker extends Worker
{
    private static final Logger LOG = Worker.buildLogger(UpdateUserWorker.class.getName());
    private final Config config;

    public UpdateUserWorker(Config config)
    {
        super();
        this.config = config;
    }

    @Override
    public void perform() throws Exception
    {
        // TODO: implement
        LOG.info("hello");
        TimeUnit.SECONDS.sleep(1);
    }
}

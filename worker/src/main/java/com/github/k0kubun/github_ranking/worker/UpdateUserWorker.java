package com.github.k0kubun.github_ranking.worker;

import com.github.k0kubun.github_ranking.config.Config;
import java.util.concurrent.TimeUnit;

public class UpdateUserWorker extends Worker
{
    private final Config config;

    public UpdateUserWorker(Config config)
    {
        super();
        this.config = config;
    }

    private void perform() throws Exception
    {
        // TODO: implement
        TimeUnit.SECONDS.sleep(1);
    }
}

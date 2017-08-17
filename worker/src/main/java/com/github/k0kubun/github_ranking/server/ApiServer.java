package com.github.k0kubun.github_ranking.server;

import com.github.k0kubun.github_ranking.config.Config;
import com.github.k0kubun.github_ranking.config.QueueConfig;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.PathHandler;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.ServletInfo;
import javax.servlet.ServletException;
import javax.sql.DataSource;
import javax.ws.rs.core.Application;
import org.jboss.resteasy.core.Dispatcher;
import org.jboss.resteasy.plugins.server.servlet.HttpServlet30Dispatcher;
import org.jboss.resteasy.spi.ResteasyDeployment;

public class ApiServer
{
    private final Class<? extends Application> applicationClass;
    private final Config config;
    private Undertow server;

    public ApiServer(Class<? extends Application> applicationClass, Config config)
    {
        this.applicationClass = applicationClass;
        this.config = config;
    }

    public void start()
    {
        ResteasyDeployment resteasyDeployment = new ResteasyDeployment();
        DeploymentInfo deploymentInfo = buildDeploymentInfo(resteasyDeployment);

        HttpHandler handler = deployHttpHandler(deploymentInfo);
        setContextObjects(resteasyDeployment);

        server = Undertow.builder()
            .addHttpListener(config.getServerPort(), config.getServerAddr())
            .setHandler(handler)
            .build();
        server.start();
    }

    public void stop()
    {
        server.stop();
    }

    private DeploymentInfo buildDeploymentInfo(ResteasyDeployment resteasyDeployment)
    {
        resteasyDeployment.setApplicationClass(applicationClass.getName());
        ServletInfo servletInfo = Servlets.servlet("ResteasyServlet", HttpServlet30Dispatcher.class)
            .addMapping("/*").setLoadOnStartup(1);

        return Servlets.deployment()
            .setDeploymentName("github-ranking-api-server")
            .setContextPath("")
            .setClassLoader(applicationClass.getClassLoader())
            .addServletContextAttribute(ResteasyDeployment.class.getName(), resteasyDeployment)
            .addServlets(servletInfo);
    }

    private HttpHandler deployHttpHandler(DeploymentInfo deploymentInfo)
    {
        DeploymentManager deploymentManager = Servlets.defaultContainer().addDeployment(deploymentInfo);
        deploymentManager.deploy();

        PathHandler pathHandler = new PathHandler();
        try {
            pathHandler.addPrefixPath(deploymentInfo.getContextPath(), deploymentManager.start());
        }
        catch (ServletException e) {
            throw new RuntimeException(e);
        }
        return pathHandler;
    }

    // Call this method after deployment because dispatcher isn't available until deployed.
    private void setContextObjects(ResteasyDeployment resteasyDeployment)
    {
        Dispatcher dispatcher = resteasyDeployment.getDispatcher();
        if (dispatcher == null) {
            throw new RuntimeException("dispatcher was null! Use setLoadOnStartup and call this method after deployment.");
        }

        // This will be available via @Context annotation.
        dispatcher.getDefaultContextObjects().put(QueueConfig.class, config.getQueueConfig());
    }
}

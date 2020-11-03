package edu.utah.kmm.emerse.solr;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import javax.annotation.PostConstruct;

/**
 * Thread pool for indexing daemons. Subclasses Spring's ThreadPoolTaskExecutor to make it easier to inject settings.
 */
public class DaemonThreadPool extends ThreadPoolTaskExecutor {

    @Value("${solr.server.daemons:1}")
    private int maxDaemons;

    @PostConstruct
    private void init() {
        setCorePoolSize(maxDaemons);
        setMaxPoolSize(maxDaemons * 2);
        setKeepAliveSeconds(60);
    }

}

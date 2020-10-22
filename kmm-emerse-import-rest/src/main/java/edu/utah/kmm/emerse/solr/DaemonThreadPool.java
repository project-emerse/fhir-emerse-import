package edu.utah.kmm.emerse.solr;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import javax.annotation.PostConstruct;

/**
 * Subclass Spring's ThreadPoolTaskExecutor to make it easier to inject settings.
 */
public class DaemonThreadPool extends ThreadPoolTaskExecutor {

    @Value("${thread_pool.corePoolSize:1}")
    private int corePoolSize = 1;

    @Value("${thread_pool.maxPoolSize:0x7fffffff}")
    private int maxPoolSize = 0x7fffffff;

    @Value("${thread_pool.keepAliveSeconds:60}")
    private int keepAliveSeconds = 60;

    @Value("${thread_pool.queueCapacity:0x7fffffff}")
    private int queueCapacity = 0x7fffffff;

    @Value("${thread_pool.allowCoreThreadTimeout:false}")
    private boolean allowCoreThreadTimeOut = false;

    @PostConstruct
    private void init() {
        setCorePoolSize(corePoolSize);
        setMaxPoolSize(maxPoolSize);
        setKeepAliveSeconds(keepAliveSeconds);
        setQueueCapacity(queueCapacity);
        setAllowCoreThreadTimeOut(allowCoreThreadTimeOut);
    }

}

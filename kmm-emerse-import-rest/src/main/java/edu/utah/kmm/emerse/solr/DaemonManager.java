package edu.utah.kmm.emerse.solr;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Manages indexing daemons.
 */
public class DaemonManager {

    private static final Log log = LogFactory.getLog(DaemonManager.class);

    @Autowired
    private SolrService solrService;

    @Autowired
    private IndexRequestQueue solrQueue;

    @Autowired
    private ThreadPoolTaskExecutor daemonThreadPool;

    @Value("${solr.server.daemons:1}")
    private int maxDaemons;

    private final List<IndexDaemon> daemons = new ArrayList<>();

    @PostConstruct
    private int startDaemons() {
        log.info("Starting indexing daemon(s).");

        int count = 0;

        for (int i = daemons.size(); i < maxDaemons; i++) {
            count++;
            IndexDaemon daemon = new IndexDaemon(i + 1, solrQueue, solrService);
            daemons.add(daemon);
            daemonThreadPool.execute(daemon);
        }

        log.info("Started " + count + " indexing daemon(s).");
        return count;
    }

    @PreDestroy
    private int stopDaemons() {
        Iterator<IndexDaemon> daemons = this.daemons.iterator();
        int count = 0;

        while (daemons.hasNext()) {
            daemons.next().terminate();
            daemons.remove();
            count++;
        }

        return count;
    }

    public int restartDaemons() {
        stopDaemons();
        return startDaemons();
    }

}

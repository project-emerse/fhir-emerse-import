package edu.utah.kmm.emerse.solr;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class DaemonManager {

    private static final Log log = LogFactory.getLog(DaemonManager.class);

    @Autowired
    private SolrService solrService;

    @Autowired
    private IndexRequestQueue solrQueue;

    @Autowired
    private DaemonThreadPool daemonThreadPool;

    @Value("${solr.server.daemons:1}")
    private int maxDaemons;

    private final List<IndexDaemon> daemons = new ArrayList<>();

    @PostConstruct
    public int startBackgroundProcessors() {
        log.info("Starting indexing daemon(s).");

        int count = 0;

        for (int i = daemons.size(); i < maxDaemons; i++) {
            count++;
            daemonThreadPool.execute(new IndexDaemon(i + 1, solrQueue, solrService));
        }

        log.info("Started " + count + " indexing daemon(s).");
        return count;
    }

    @PreDestroy
    public int stopBackgroundProcessors() {
        Iterator<IndexDaemon> daemons = this.daemons.iterator();
        int count = 0;

        while (daemons.hasNext()) {
            daemons.next().terminate();
            daemons.remove();
            count++;
        }

        return count;
    }
}

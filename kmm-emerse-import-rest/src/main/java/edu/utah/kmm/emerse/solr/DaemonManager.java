package edu.utah.kmm.emerse.solr;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class DaemonManager {

    private static final Log log = LogFactory.getLog(DaemonManager.class);

    @Autowired
    private SolrService solrService;

    @Autowired
    private IndexRequestQueue solrQueue;

    @Value("${solr.server.daemons:1}")
    private int maxDaemons;

    private final List<IndexDaemon> daemons = new ArrayList<>();

    public void init() {
        startBackgroundProcessors();
    }

    public void destroy() {
        stopBackgroundProcessors();
    }

    public int startBackgroundProcessors() {
        log.info("Starting indexing daemon(s).");

        int count = 0;

        for (int i = daemons.size(); i < maxDaemons; i++) {
            daemons.add(new IndexDaemon(solrQueue, solrService));
            count++;
        }

        log.info("Started " + count + "indexing daemon(s).");
        return count;
    }

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

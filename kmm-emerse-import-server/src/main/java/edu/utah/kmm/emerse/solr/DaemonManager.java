package edu.utah.kmm.emerse.solr;

import edu.utah.kmm.emerse.database.DatabaseService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class DaemonManager {

    private static Log log = LogFactory.getLog(DaemonManager.class);

    @Autowired
    private SolrService solrService;

    @Autowired
    private SolrQueue solrQueue;

    @Autowired
    private DatabaseService databaseService;

    private final int maxDaemons;

    private final List<IndexDaemon> daemons = new ArrayList<>();

    public DaemonManager(int maxDaemons) {
        this.maxDaemons = maxDaemons;
    }

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

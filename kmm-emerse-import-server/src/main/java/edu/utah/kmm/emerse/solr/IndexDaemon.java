package edu.utah.kmm.emerse.solr;

import edu.utah.kmm.emerse.dto.IndexRequestDTO;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class IndexDaemon implements Runnable {

    private static Log log = LogFactory.getLog(IndexDaemon.class);

    private static int daemonCounter;

    private final SolrQueue solrQueue;

    private final SolrService solrService;

    private final Thread thread;

    private final int daemonId = ++daemonCounter;

    private boolean terminate;

    private boolean terminated;

    private boolean running;

    IndexDaemon(SolrQueue solrQueue, SolrService solrService) {
        this.solrQueue = solrQueue;
        this.solrService = solrService;
        thread = new Thread(this);
        thread.setName("Solr indexing daemon #" + daemonId);
        thread.start();
    }

    public void terminate() {
        this.terminate = true;
    }

    public boolean isTerminated() {
        return terminated;
    }

    @Override
    public void run() {
        log.info("Started indexing daemon #" + daemonId);
        running = true;

        while (!terminated && !thread.isInterrupted()) {
            IndexRequestDTO request = solrQueue.nextRequest();

            if (request == null) {
                try {
                    thread.sleep(5000);
                } catch (InterruptedException e) {
                    terminated = true;
                }
            } else {
                solrService.index(request);
            }
        }

        log.info("Stopped indexing daemon #" + daemonId);
    }

}

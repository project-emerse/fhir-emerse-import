package edu.utah.kmm.emerse.solr;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class IndexDaemon implements Runnable {

    private static Log log = LogFactory.getLog(IndexDaemon.class);

    private static int daemonCounter;

    private final IndexRequestQueue indexRequestQueue;

    private final SolrService solrService;

    private final Thread thread;

    private final int daemonId = ++daemonCounter;

    private boolean terminated;

    private boolean running;

    IndexDaemon(IndexRequestQueue indexRequestQueue, SolrService solrService) {
        this.indexRequestQueue = indexRequestQueue;
        this.solrService = solrService;
        thread = new Thread(this);
        thread.setName("EMERSE-IT indexing daemon #" + daemonId);
        thread.start();
    }

    public void terminate() {
        this.terminated = true;
    }

    public boolean isTerminated() {
        return terminated;
    }

    public boolean isRunning() {
        return running;
    }

    @Override
    public void run() {
        log.info("Started indexing daemon #" + daemonId);
        running = true;

        while (!terminated && !thread.isInterrupted()) {
            String requestId = indexRequestQueue.nextRequest();

            if (requestId == null) {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    terminated = true;
                }
            } else {
                try {
                    solrService.processRequest(requestId);
                } catch (Exception e) {
                    log.error(e);
                }
            }
        }

        running = false;
        log.info("Stopped indexing daemon #" + daemonId);
    }

}

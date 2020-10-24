package edu.utah.kmm.emerse.solr;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class IndexDaemon implements Runnable {

    private static Log log = LogFactory.getLog(IndexDaemon.class);

    private final IndexRequestQueue indexRequestQueue;

    private final SolrService solrService;

    private final int daemonId;

    private boolean terminated;

    IndexDaemon(int daemonId, IndexRequestQueue indexRequestQueue, SolrService solrService) {
        this.daemonId = daemonId;
        this.indexRequestQueue = indexRequestQueue;
        this.solrService = solrService;
    }

    public void terminate() {
        this.terminated = true;
    }

    @Override
    public void run() {
        Thread thread = Thread.currentThread();
        String threadName = "EMERSE-IT indexing daemon #" + daemonId;
        thread.setName(threadName);
        log.info("Started " + threadName);

        while (!terminated && !thread.isInterrupted()) {
            IndexRequestWrapper wrapper = indexRequestQueue.nextRequest();

            if (wrapper == null) {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    terminated = true;
                }
            } else {
                try {
                    solrService.processRequest(wrapper.get());
                } catch (Exception e) {
                    log.error(e);
                }
            }
        }

        log.info("Stopped " + threadName);
    }

}

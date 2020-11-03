package edu.utah.kmm.emerse.solr;

import edu.utah.kmm.emerse.database.DatabaseService;
import edu.utah.kmm.emerse.util.MiscUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.RowMapper;

import javax.annotation.PostConstruct;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * A queue of index requests awaiting processing.
 */
public class IndexRequestQueue implements RowMapper<String> {

    private static final Log log = LogFactory.getLog(IndexRequestQueue.class);

    @Autowired
    private DatabaseService databaseService;

    private final Queue<IndexRequestWrapper> queue = new LinkedBlockingQueue<>();

    @Autowired
    private IndexRequestFactory indexRequestFactory;

    @Value("${solr.queue.polling.interval:60000}")
    private int pollingInterval;

    private volatile long nextPoll;

    public IndexRequestQueue() {
    }

    @PostConstruct
    private void postConstruct() {
        if (pollingInterval < 10000) {
            log.warn("Solr queue polling interval (" + pollingInterval + " ms) was set to minimum threshold of 10000 ms.");
            pollingInterval = 10000;
        }
    }

    /**
     * Returns the next request from the queue.  If there are no more requests and the polling interval has been
     * exceeded, the database will be queried for new entries.
     *
     * @return The next request from the queue (possibly null).
     */
    IndexRequestWrapper nextRequest() {
        synchronized (queue) {
            long currentTime = System.currentTimeMillis();

            if (queue.isEmpty() && currentTime > nextPoll) {
                nextPoll = currentTime + pollingInterval;
                databaseService.refreshQueue(this);
            }

            return queue.isEmpty() ? null : queue.remove();
        }
    }

    /**
     * Forces the queue to be refreshed immediately once it is empty.
     */
    public void refreshNow() {
        this.nextPoll = 0;
    }

    /**
     * Populates the queue from a result set.
     */
    @Override
    public String mapRow(
            ResultSet rs,
            int i) {
        try {
            String id = rs.getString("ID");
            queue.add(indexRequestFactory.create(id, true));
            return id;
        } catch (SQLException e) {
            return MiscUtil.rethrow(e);
        }
    }
}

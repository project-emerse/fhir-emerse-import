package edu.utah.kmm.emerse.solr;

import edu.utah.kmm.emerse.database.DatabaseService;
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

public class IndexRequestQueue implements RowMapper<String> {

    private static final Log log = LogFactory.getLog(IndexRequestQueue.class);

    @Autowired
    private DatabaseService databaseService;

    private final Queue<IndexRequestWrapper> queue = new LinkedBlockingQueue<>();

    @Autowired
    private IndexRequestFactory indexRequestFactory;

    @Value("${solr.queue.refresh.interval:60000}")
    private int refreshInterval;

    private volatile long nextRefresh;

    public IndexRequestQueue() {
    }

    @PostConstruct
    private void postConstruct() {
        if (refreshInterval < 10000) {
            log.warn("Solr queue refresh interval (" + refreshInterval + " ms) was set to minimum threshold of 10000 ms.");
            refreshInterval = 10000;
        }
    }

    IndexRequestWrapper nextRequest() {
        synchronized (queue) {
            long currentTime = System.currentTimeMillis();

            if (queue.isEmpty() && currentTime > nextRefresh) {
                nextRefresh = currentTime + refreshInterval;
                databaseService.refreshQueue(this);
            }

            return queue.isEmpty() ? null : queue.remove();
        }
    }

    public void refreshNow() {
        this.nextRefresh = 0;
    }

    @Override
    public String mapRow(
            ResultSet rs,
            int i) {
        try {
            String id = rs.getString("ID");
            queue.add(indexRequestFactory.create(id, true));
            return id;
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}

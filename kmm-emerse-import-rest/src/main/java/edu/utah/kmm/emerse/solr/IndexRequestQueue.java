package edu.utah.kmm.emerse.solr;

import edu.utah.kmm.emerse.database.DatabaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public class IndexRequestQueue implements RowMapper<String> {

    @Autowired
    private DatabaseService databaseService;

    private final Queue<String> queue = new LinkedBlockingQueue<>();

    private long nextRefresh;

    public IndexRequestQueue() {
    }

    String nextRequest() {
        synchronized (queue) {
            long currentTime = System.currentTimeMillis();

            if (queue.isEmpty() && currentTime > nextRefresh) {
                nextRefresh = currentTime + 60000;
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
            queue.add(id);
            return id;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}

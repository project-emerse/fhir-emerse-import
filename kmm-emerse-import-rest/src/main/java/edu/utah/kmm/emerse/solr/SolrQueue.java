package edu.utah.kmm.emerse.solr;

import edu.utah.kmm.emerse.database.DatabaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public class SolrQueue implements RowMapper {

    @Autowired
    private DatabaseService databaseService;

    private Queue<IndexRequestDTO> queue = new LinkedBlockingQueue<>();

    private long nextRefresh;

    public SolrQueue() {
    }

    IndexRequestDTO nextRequest() {
        synchronized (queue) {
            long currentTime = System.currentTimeMillis();

            if (queue.isEmpty() && currentTime > nextRefresh) {
                nextRefresh = currentTime + 60000;
                databaseService.refreshQueue(this);
            }

            return queue.isEmpty() ? null : queue.remove();
        }
    }

    @Override
    public IndexRequestDTO mapRow(ResultSet rs, int i) {
        IndexRequestDTO entry = new IndexRequestDTO(rs);
        queue.add(entry);
        return entry;
    }
}

package edu.utah.kmm.emerse.solr;

import edu.utah.kmm.emerse.database.DatabaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;

import java.util.HashMap;
import java.util.Map;

public class IndexRequestFactory {

    private final Map<String, IndexRequestWrapper> cache = new HashMap<>();

    @Value("${server.uuid}")
    private String serverId;

    @Autowired
    private DatabaseService databaseService;

    private final IndexRequestDTO.ICloseCallback teardown = request -> {
        try {
            databaseService.updateIndexRequest(request);
        } finally {
            remove(request.getId());
        }
    };

    public synchronized IndexRequestWrapper create(Resource resource) {
        IndexRequestWrapper wrapper = new IndexRequestWrapper(resource, serverId, teardown);
        cache.put(wrapper.getIndexRequestId(), wrapper);
        return wrapper;
    }

    public synchronized IndexRequestWrapper create(
            String indexRequestId,
            boolean autoFetch) {
        IndexRequestWrapper wrapper = cache.get(indexRequestId);

        if (wrapper == null && autoFetch) {
            cache.put(indexRequestId, wrapper = new IndexRequestWrapper(indexRequestId, teardown,
                    () -> databaseService.fetchRequest(indexRequestId)));
        }

        return wrapper;
    }

    public synchronized boolean remove(String indexRequestId) {
        return cache.remove(indexRequestId) != null;
    }

}

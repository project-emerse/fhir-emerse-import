package edu.utah.kmm.emerse.solr;

import edu.utah.kmm.emerse.database.DatabaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;

import java.util.HashMap;
import java.util.Map;

public class IndexRequestFactory {

    private final Map<String, IndexRequestWrapper> cache = new HashMap<>();

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
        IndexRequestWrapper wrapper = new IndexRequestWrapper(resource, teardown);
        cache.put(wrapper.getIndexRequestId(), wrapper);
        return wrapper;
    }

    public synchronized IndexRequestWrapper create(
            String indexRequestId,
            boolean force) {
        IndexRequestWrapper wrapper = cache.get(indexRequestId);

        if (wrapper == null && force) {
            cache.put(indexRequestId, wrapper = new IndexRequestWrapper(indexRequestId, teardown,
                    () -> databaseService.fetchRequest(indexRequestId)));
        }

        return wrapper;
    }

    public synchronized boolean remove(String indexRequestId) {
        return cache.remove(indexRequestId) != null;
    }

}

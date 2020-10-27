package edu.utah.kmm.emerse.solr;

import edu.utah.kmm.emerse.solr.IndexRequestDTO.ICloseCallback;
import org.codehaus.janino.util.Producer;
import org.springframework.core.io.Resource;

public class IndexRequestWrapper {

    private final String indexRequestId;

    private final Producer<IndexRequestDTO> factory;

    private final ICloseCallback teardown;

    private volatile IndexRequestDTO indexRequestDTO;

    IndexRequestWrapper(
            String indexRequestId,
            ICloseCallback teardown,
            Producer<IndexRequestDTO> factory
    ) {
        this.indexRequestId = indexRequestId;
        this.teardown = teardown;
        this.factory = factory;
    }

    IndexRequestWrapper(
            Resource resource,
            String serverId,
            ICloseCallback teardown) {
        indexRequestDTO = new IndexRequestDTO(resource, serverId);
        this.indexRequestId = indexRequestDTO.getId();
        this.teardown = teardown;
        this.factory = null;
        addCloseCallbacks();
    }

    public String getIndexRequestId() {
        return indexRequestId;
    }

    public boolean isHydrated() {
        return indexRequestDTO != null;
    }

    public IndexRequestDTO get() {
        return indexRequestDTO == null ? fetch() : indexRequestDTO;
    }

    private synchronized IndexRequestDTO fetch() {
        if (indexRequestDTO == null) {
            indexRequestDTO = factory.produce();
            addCloseCallbacks();
        }

        return indexRequestDTO;
    }

    private void addCloseCallbacks() {
        indexRequestDTO.registerCloseCallback(teardown);
    }

}

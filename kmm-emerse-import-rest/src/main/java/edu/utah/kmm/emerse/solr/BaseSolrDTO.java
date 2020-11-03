package edu.utah.kmm.emerse.solr;

import edu.utah.kmm.emerse.database.BaseDTO;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Abstract base class for all DTO objects that can also be indexed.
 */
public abstract class BaseSolrDTO extends BaseDTO {

    protected final Map<String, String> solrKeyMappings;

    protected BaseSolrDTO(
            Map<String, Object> additionalParams,
            Map<String, String> solrKeyMappings) {
        super(additionalParams);
        this.solrKeyMappings = solrKeyMappings;
    }

    /**
     * Returns the map to be used by the SOLR indexer.  If no SOLR key mappings are provided, returns
     * the original DTO map.  Otherwise, returns the DTO map as transformed by the provided key mappings.
     */
    public Map<String, Object> getSolrMap() {
        if (solrKeyMappings == null) {
            return Collections.unmodifiableMap(map);
        }

        Map<String, Object> solrMap = new HashMap<>(map);

        for (Map.Entry<String, String> entry : solrKeyMappings.entrySet()) {
            String oldKey = entry.getKey();
            String newKey = entry.getValue();

            if (newKey == null) {
                solrMap.remove(oldKey);
            } else if (solrMap.containsKey(oldKey)) {
                solrMap.put(newKey, solrMap.remove(oldKey));
            }
        }

        return Collections.unmodifiableMap(solrMap);
    }

}

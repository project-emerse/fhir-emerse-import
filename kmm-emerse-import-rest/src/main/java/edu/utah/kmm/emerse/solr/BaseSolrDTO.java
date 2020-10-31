package edu.utah.kmm.emerse.solr;

import edu.utah.kmm.emerse.database.BaseDTO;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Abstract base class for all DTO objects.
 */
public abstract class BaseSolrDTO extends BaseDTO {

    protected final Map<String, String> solrKeyMappings;

    protected BaseSolrDTO(
            Map<String, Object> additionalParams,
            Map<String, String> solrKeyMappings) {
        super(additionalParams);
        this.solrKeyMappings = solrKeyMappings;
    }

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

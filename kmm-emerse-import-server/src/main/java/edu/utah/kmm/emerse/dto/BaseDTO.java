package edu.utah.kmm.emerse.dto;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Abstract base class for all DTO objects.
 */
public abstract class BaseDTO {

    protected final Map<String, Object> map = new HashMap<>();

    protected BaseDTO() {
    }

    protected BaseDTO(Map<String, Object> additionalParams) {
        if (additionalParams != null) {
            map.putAll(additionalParams);
        }
    }

    public Map<String, Object> getMap() {
        return Collections.unmodifiableMap(map);
    }
}

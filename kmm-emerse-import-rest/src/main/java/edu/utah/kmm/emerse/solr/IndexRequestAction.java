package edu.utah.kmm.emerse.solr;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Encapsulates an action to be performed on a specific index request.
 */
public class IndexRequestAction {

    /**
     * Types of actions that may be performed on an index request.
     */
    public enum Action {
        DELETE, RESUME, SUSPEND, ABORT, RESTART
    }

    @JsonProperty(required = true)
    public String id;

    @JsonProperty(required = true)
    public Action action;

}

package edu.utah.kmm.emerse.solr;

import com.fasterxml.jackson.annotation.JsonProperty;

public class IndexRequestAction {

    public enum Action {
        DELETE, RESUME, SUSPEND, ABORT
    }

    @JsonProperty(required = true)
    public String id;

    @JsonProperty(required = true)
    public Action action;

}

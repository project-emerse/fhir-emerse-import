package edu.utah.kmm.emerse;

import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient.Builder;
import org.apache.solr.client.solrj.impl.XMLResponseParser;

public class SolrClientFactory {

    public static HttpSolrClient newSolrClient(String baseSolrUrl) {
        return new Builder(baseSolrUrl)
            .withResponseParser(new XMLResponseParser())
            .allowCompression(true)
            .build();
    }
}

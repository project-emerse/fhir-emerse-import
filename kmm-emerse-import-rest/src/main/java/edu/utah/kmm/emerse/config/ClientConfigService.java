package edu.utah.kmm.emerse.config;

import edu.utah.kmm.emerse.database.DatabaseService;
import edu.utah.kmm.emerse.solr.SolrService;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.janino.util.Producer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.ReflectionUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Returns selected configuration data to send to client.
 */
@SuppressWarnings("unused")
public class ClientConfigService {

    private final String _serverVersion;

    @Autowired
    private DatabaseService databaseService;

    @Autowired
    private SolrService solrService;

    @Value("${fhir.mrn.system}")
    private String s1;

    @Value("${emerse.home.url}")
    private String s2;

    @Value("${app.timeout.seconds}")
    private String s3;

    @Value("${app.refresh.seconds:10}")
    private String s4;

    @Value("${solr.server.daemons:1}")
    private String s5;

    @Value("${server.uuid}")
    private String s6;

    @Value("${fhir.server.authtype}")
    private String s7;

    @Value("${fhir.server.patientlookup:DEFAULT}")
    private String s8;

    @Value("${server.version:}")
    private String serverVersion;

    @Value("${solr.version:}")
    private String solrVersion;

    @Value("${database.version:}")
    private String databaseVersion;

    private ClientConfigService(String serverVersion) {
        this._serverVersion = formatServerVersion(serverVersion);
    }

    private String formatServerVersion(String serverVersion) {
        String[] pcs = serverVersion.split(",", 2);
        return pcs[0] + " - " + pcs[1].replace("Z", "").replace("T", " ");
    }

    public Map<String, String> getConfig() {
        serverVersion = StringUtils.firstNonBlank(serverVersion, _serverVersion);
        solrVersion = firstNonBlank(solrVersion, () -> solrService.getSolrVersion());
        databaseVersion = firstNonBlank(databaseVersion, () -> databaseService.getDatabaseVersion());
        Map<String, String> config = new HashMap<>();
        ReflectionUtils.doWithLocalFields(getClass(), field -> {
            Value annot = field.getAnnotation(Value.class);

            if (annot != null) {
                String key = StringUtils.substringBetween(annot.value(), "{", "}");
                key = StringUtils.substringBefore(key, ":");
                config.put(key, (String) field.get(this));
            }
        });

        return config;
    }

    private String firstNonBlank(
            String value,
            Producer<String> producer) {
        return StringUtils.isBlank(value) ? producer.produce() : value;
    }

}

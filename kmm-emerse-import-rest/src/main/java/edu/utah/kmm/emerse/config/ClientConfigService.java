package edu.utah.kmm.emerse.config;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.ReflectionUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Returns selected configuration data to send to client.
 */
public class ClientConfigService {

    @Value("${fhir.mrn.system}")
    private String s1;

    @Value("${emerse.home.url}")
    private String s2;

    @Value("${app.timeout.seconds}")
    private String s3;

    @Value("${solr.server.daemons:1}")
    private String s4;

    public Map<String, String> getConfig() {
        Map<String, String> config = new HashMap<>();
        ReflectionUtils.doWithLocalFields(getClass(), field -> {
            Value annot = field.getAnnotation(Value.class);

            if (annot != null) {
                String key = StringUtils.substringBetween(annot.value(), "{", "}");
                config.put(key, (String) field.get(this));
            }
        });

        return config;
    }

}

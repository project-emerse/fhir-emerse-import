package edu.utah.kmm.emerse.config;

import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;

import java.util.HashMap;
import java.util.Map;

public class ConfigService implements EnvironmentAware {

    private static final String[] CONFIG_SETTINGS = {
        "fhir.mrn.system",
        "emerse.home.url"
    };

    private Environment environment;

    public Map<String, String> getConfig() {
        Map<String, String> config = new HashMap<>();

        for (String name: CONFIG_SETTINGS) {
            config.put(name, environment.getProperty(name));
        }

        return config;
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }
}

package edu.utah.kmm.emerse.database;

import edu.utah.kmm.emerse.security.Credentials;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

public abstract class AbstractDataSourceFactory {

    protected final String driverClass;

    protected final String connectionUrl;

    protected final Credentials credentials;

    @Value("${solr.server.daemons:1}")
    private int maxDaemons;

    protected AbstractDataSourceFactory(
            String driverClass,
            String connectionUrl,
            Credentials credentials
    ) {
        this.driverClass = driverClass;
        this.connectionUrl = connectionUrl;
        this.credentials = credentials;
    }

    @PostConstruct
    private void postConstruct() throws Exception {
        init(maxDaemons + 2);
    }

    protected abstract void init(int connectionPoolSize) throws Exception;

    public abstract DataSource getDataSource();

}

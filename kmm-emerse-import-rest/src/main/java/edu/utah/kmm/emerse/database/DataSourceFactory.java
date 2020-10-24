package edu.utah.kmm.emerse.database;

import edu.utah.kmm.emerse.security.Credentials;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;

public class DataSourceFactory extends AbstractDataSourceFactory {

    private DataSource datasource;

    public DataSourceFactory(
            String driverClass,
            String connectionUrl,
            Credentials credentials
    ) {
        super(driverClass, connectionUrl, credentials);
    }

    protected void init(int connectionPoolSize) {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName(driverClass);
        ds.setUrl(connectionUrl);
        ds.setUsername(credentials.getUsername());
        ds.setPassword(credentials.getPassword());
        datasource = ds;
    }

    public DataSource getDataSource() {
        return datasource;
    }

}

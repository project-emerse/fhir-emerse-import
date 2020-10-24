package edu.utah.kmm.emerse.database;

import edu.utah.kmm.emerse.security.Credentials;
import org.apache.commons.dbcp2.BasicDataSource;

import javax.sql.DataSource;

public class DataSourceFactory4 extends AbstractDataSourceFactory {

    private DataSource datasource;

    public DataSourceFactory4(
            String driverClass,
            String connectionUrl,
            Credentials credentials
    ) {
        super(driverClass, connectionUrl, credentials);
    }

    @Override
    protected void init(int connectionPoolSize) {
        BasicDataSource ds = new BasicDataSource();
        ds.setDriverClassName(driverClass);
        ds.setUrl(connectionUrl);
        ds.setUsername(credentials.getUsername());
        ds.setPassword(credentials.getPassword());
        ds.setInitialSize(connectionPoolSize);
        datasource = ds;
    }

    public DataSource getDataSource() {
        return datasource;
    }

}

package edu.utah.kmm.emerse.database;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import edu.utah.kmm.emerse.security.Credentials;

import javax.sql.DataSource;

/**
 * Data source factory using C3P0 library.
 */
public class DataSourceFactory5 extends AbstractDataSourceFactory {

    private DataSource datasource;

    public DataSourceFactory5(
            String driverClass,
            String connectionUrl,
            Credentials credentials
    ) {
        super(driverClass, connectionUrl, credentials);
    }

    @Override
    protected void init(int connectionPoolSize) throws Exception {
        ComboPooledDataSource ds = new ComboPooledDataSource();
        ds.setDriverClass(driverClass);
        ds.setJdbcUrl(connectionUrl);
        ds.setUser(credentials.getUsername());
        ds.setPassword(credentials.getPassword());
        ds.setInitialPoolSize(connectionPoolSize);
        ds.setMaxPoolSize(connectionPoolSize);
        ds.setAutoCommitOnClose(true);
        datasource = ds;
    }

    public DataSource getDataSource() {
        return datasource;
    }

}

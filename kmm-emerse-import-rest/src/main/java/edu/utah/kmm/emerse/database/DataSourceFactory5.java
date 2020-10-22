package edu.utah.kmm.emerse.database;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import edu.utah.kmm.emerse.security.Credentials;

import javax.sql.DataSource;

public class DataSourceFactory5 {

    private final DataSource datasource;

    public DataSourceFactory5(
            String driverClass,
            String connectionUrl,
            Credentials credentials
    ) throws Exception {
        ComboPooledDataSource ds = new ComboPooledDataSource();
        ds.setDriverClass(driverClass);
        ds.setJdbcUrl(connectionUrl);
        ds.setUser(credentials.getUsername());
        ds.setPassword(credentials.getPassword());
        ds.setAutoCommitOnClose(true);
        datasource = ds;
    }

    public DataSource getDataSource() {
        return datasource;
    }

}

package edu.utah.kmm.emerse.database;

import edu.utah.kmm.emerse.security.Credentials;
import org.apache.commons.dbcp2.BasicDataSource;

import javax.sql.DataSource;

public class DataSourceFactory4 {

    private final DataSource datasource;

    public DataSourceFactory4(
            String driverClass,
            String connectionUrl,
            Credentials credentials
    ) {
        BasicDataSource ds = new BasicDataSource();
        ds.setDriverClassName(driverClass);
        ds.setUrl(connectionUrl);
        ds.setUsername(credentials.getUsername());
        ds.setPassword(credentials.getPassword());
        ds.setMinIdle(5);
        ds.setMaxIdle(10);
        ds.setMaxOpenPreparedStatements(100);
        ds.setAutoCommitOnReturn(true);
        datasource = ds;
    }

    public DataSource getDataSource() {
        return datasource;
    }

}

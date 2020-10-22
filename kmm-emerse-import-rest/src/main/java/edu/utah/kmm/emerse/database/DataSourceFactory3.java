package edu.utah.kmm.emerse.database;

import edu.utah.kmm.emerse.security.Credentials;
import org.apache.commons.dbcp2.cpdsadapter.DriverAdapterCPDS;
import org.apache.commons.dbcp2.datasources.SharedPoolDataSource;

import javax.sql.DataSource;

public class DataSourceFactory3 {

    private final DriverAdapterCPDS driver;

    public DataSourceFactory3(
            String driverClass,
            String connectionUrl,
            Credentials credentials
    ) throws Exception {
        driver = new DriverAdapterCPDS();
        driver.setDriver(driverClass);
        driver.setUrl(connectionUrl);
        driver.setUser(credentials.getUsername());
        driver.setPassword(credentials.getPassword());
    }

    public DataSource getDataSource() {
        SharedPoolDataSource ds = new SharedPoolDataSource();
        ds.setConnectionPoolDataSource(driver);
        ds.setDefaultMaxTotal(10);
        ds.setDefaultMaxWaitMillis(50000);
        ds.setDefaultTestOnBorrow(true);
        ds.setValidationQuery("SELECT 1");
        ds.setDefaultTestWhileIdle(true);
        ds.setDefaultAutoCommit(true);
        return ds;
    }

}

package edu.utah.kmm.emerse.database;

import edu.utah.kmm.emerse.security.Credentials;
import org.apache.commons.dbcp2.cpdsadapter.DriverAdapterCPDS;
import org.apache.commons.dbcp2.datasources.SharedPoolDataSource;

import javax.sql.DataSource;

public class DataSourceFactory3 extends AbstractDataSourceFactory {

    private DataSource dataSource;

    public DataSourceFactory3(
            String driverClass,
            String connectionUrl,
            Credentials credentials
    ) {
        super(driverClass, connectionUrl, credentials);
    }

    protected void init(int connectionPoolSize) throws Exception {
        DriverAdapterCPDS driver = new DriverAdapterCPDS();
        driver.setDriver(driverClass);
        driver.setUrl(connectionUrl);
        driver.setUser(credentials.getUsername());
        driver.setPassword(credentials.getPassword());
        SharedPoolDataSource ds = new SharedPoolDataSource();
        ds.setConnectionPoolDataSource(driver);
        ds.setDefaultMaxTotal(connectionPoolSize);
        ds.setDefaultMaxWaitMillis(50000);
        ds.setDefaultTestOnBorrow(true);
        ds.setValidationQuery("SELECT 1");
        ds.setDefaultTestWhileIdle(true);
        ds.setDefaultAutoCommit(true);
        dataSource = ds;
    }

    public DataSource getDataSource() {
        return dataSource;
    }

}

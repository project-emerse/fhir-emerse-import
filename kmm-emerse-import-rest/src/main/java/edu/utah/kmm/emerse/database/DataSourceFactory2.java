package edu.utah.kmm.emerse.database;

import edu.utah.kmm.emerse.security.Credentials;
import org.apache.commons.dbcp2.DriverManagerConnectionFactory;
import org.apache.commons.dbcp2.PoolableConnection;
import org.apache.commons.dbcp2.PoolableConnectionFactory;
import org.apache.commons.dbcp2.PoolingDataSource;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;

import javax.sql.DataSource;

public class DataSourceFactory2 {

    private final ObjectPool<PoolableConnection> connectionPool;

    public DataSourceFactory2(
            String driverClass,
            String connectionUrl,
            Credentials credentials
    ) throws Exception {
        Class.forName(driverClass);
        DriverManagerConnectionFactory factory = new DriverManagerConnectionFactory(connectionUrl, credentials.getUsername(), credentials.getPassword());
        PoolableConnectionFactory connectionFactory = new PoolableConnectionFactory(factory, null);
        connectionPool = new GenericObjectPool<>(connectionFactory);
        connectionFactory.setPool(connectionPool);
        connectionFactory.setAutoCommitOnReturn(true);
    }

    public DataSource getDataSource() {
        return new PoolingDataSource<>(connectionPool);
    }

}

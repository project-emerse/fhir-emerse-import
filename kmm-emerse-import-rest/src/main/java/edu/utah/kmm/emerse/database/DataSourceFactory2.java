package edu.utah.kmm.emerse.database;

import edu.utah.kmm.emerse.security.Credentials;
import org.apache.commons.dbcp2.DriverManagerConnectionFactory;
import org.apache.commons.dbcp2.PoolableConnection;
import org.apache.commons.dbcp2.PoolableConnectionFactory;
import org.apache.commons.dbcp2.PoolingDataSource;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import javax.sql.DataSource;

/**
 * Data source factory using DBCP poolable connection factory.
 */
public class DataSourceFactory2 extends AbstractDataSourceFactory {

    private ObjectPool<PoolableConnection> connectionPool;

    public DataSourceFactory2(
            String driverClass,
            String connectionUrl,
            Credentials credentials
    ) {
        super(driverClass, connectionUrl, credentials);
    }

    @Override
    protected void init(int connectionPoolSize) throws Exception {
        Class.forName(driverClass);
        DriverManagerConnectionFactory factory = new DriverManagerConnectionFactory(connectionUrl, credentials.getUsername(), credentials.getPassword());
        PoolableConnectionFactory connectionFactory = new PoolableConnectionFactory(factory, null);
        GenericObjectPoolConfig config = new GenericObjectPoolConfig();
        config.setMaxTotal(connectionPoolSize);
        connectionPool = new GenericObjectPool<>(connectionFactory, config);
        connectionFactory.setPool(connectionPool);
        connectionFactory.setAutoCommitOnReturn(true);
    }

    public DataSource getDataSource() {
        return new PoolingDataSource<>(connectionPool);
    }

}

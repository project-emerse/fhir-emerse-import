package edu.utah.kmm.emerse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

public class PatientRegistry {

    private static final Log log = LogFactory.getLog(PatientRegistry.class);

    private JdbcTemplate jdbcTemplate = new JdbcTemplate();

    @Autowired
    private DriverManagerDataSource dataSource;

    private void init() {
        jdbcTemplate.setDataSource(dataSource);
    }
}

package edu.utah.kmm.emerse.database;

import ca.uhn.fhir.model.dstu2.resource.Patient;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/**
 * Database-related services.
 */
public class DatabaseService {

    private static final Log log = LogFactory.getLog(DatabaseService.class);

    private final JdbcTemplate jdbcTemplate = new JdbcTemplate();

    private final DriverManagerDataSource dataSource;

    public DatabaseService(DriverManagerDataSource dataSource) {
        this.dataSource = dataSource;
        jdbcTemplate.setDataSource(dataSource);
    }

    /**
     * Creates or updates entry in EMERSE patient table.
     *
     * @param patient Patient resource.
     */
    public void updatePatient(Patient patient) {

    }
}

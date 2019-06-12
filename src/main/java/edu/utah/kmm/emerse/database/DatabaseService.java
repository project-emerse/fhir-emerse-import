package edu.utah.kmm.emerse.database;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hl7.fhir.dstu3.model.Patient;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Database-related services.
 */
public class DatabaseService {

    private static final Log log = LogFactory.getLog(DatabaseService.class);

    private final NamedParameterJdbcTemplate jdbcTemplate;

    private final DriverManagerDataSource dataSource;

    private BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public DatabaseService(DriverManagerDataSource dataSource) {
        this.dataSource = dataSource;
        jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
    }

    /**
     * Creates or updates entry in EMERSE patient table.
     *
     * @param patient Patient resource.
     */
    public void updatePatient(Patient patient) {

    }

    public boolean authenticate(String username, String password) {
        if (true) return !"bad".equals(username); //TEMPORARY
        try {
            MapSqlParameterSource params = new MapSqlParameterSource();
            params.addValue("username", username);
            params.addValue("password", passwordEncoder.encode(password));
            int result = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM LOGIN_ACCOUNT WHERE USER_ID = :username AND PASSWORD = :password",
                    params, Integer.class);
            return result == 1;
        } catch (Exception e) {
            log.error("Error while attempting user authentication.", e);
            return false;
        }
    }
}

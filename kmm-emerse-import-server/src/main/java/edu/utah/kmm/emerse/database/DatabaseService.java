package edu.utah.kmm.emerse.database;

import edu.utah.kmm.emerse.fhir.FhirClient;
import edu.utah.kmm.emerse.security.Credentials;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hl7.fhir.dstu3.model.HumanName;
import org.hl7.fhir.dstu3.model.Patient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Date;

/**
 * Database-related services.
 */
public class DatabaseService {

    private static final Log log = LogFactory.getLog(DatabaseService.class);

    private static final String QUEUE_TABLE = "INDEXING_QUEUE";

    private static final String[] PATIENT_UPDATE_FIELDS = {
            "EXTERNAL_ID","FIRST_NAME","MIDDLE_NAME","LAST_NAME","BIRTH_DATE","SEX_CD","DECEASED_FLAG",
            "LANGUAGE_CD","RACE_CD","ETHNICITY_CD","MARITAL_STATUS_CD","RELIGION_CD","ZIP_CD",
            "UPDATE_DATE","UPDATED_BY"
    };

    private static final String[] PATIENT_CREATE_FIELDS = {
            "EXTERNAL_ID","FIRST_NAME","MIDDLE_NAME","LAST_NAME","BIRTH_DATE","SEX_CD","DECEASED_FLAG",
            "LANGUAGE_CD","RACE_CD","ETHNICITY_CD","MARITAL_STATUS_CD","RELIGION_CD","ZIP_CD",
            "CREATE_DATE","CREATED_BY", "DELETED_FLAG"
    };

    private static final String QUEUE_CHECK = "SELECT * FROM INDEXING_QUEUE WHERE COMPLETED IS NULL ORDER BY ID ASC";

    private final NamedParameterJdbcTemplate jdbcTemplate;

    private final DriverManagerDataSource dataSource;

    private BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Autowired
    private FhirClient fhirClient;

    public DatabaseService(DriverManagerDataSource dataSource, Credentials dbCredentials) {
        this.dataSource = dataSource;
        dataSource.setUsername(dbCredentials.getUsername());
        dataSource.setPassword(dbCredentials.getPassword());
        jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
    }

    /**
     * Creates or updates entry in EMERSE patient table.
     *
     * @param patient Patient resource.
     */
    public void createOrUpdatePatient(Patient patient, boolean update) {
        String mrn = fhirClient.extractMRN(patient);
        Integer recno = getPatientRecNum(mrn);

        if (recno != null && !update) {
            return;
        }

        String SQL = recno == null ? getPatientInsertSQL() : getPatientUpdateSQL();
        HumanName name = patient.getNameFirstRep();
        boolean deceased = patient.hasDeceasedDateTimeType() || (patient.hasDeceasedBooleanType() && patient.getDeceasedBooleanType().getValue());

        MapSqlParameterSource params = new MapSqlParameterSource();

        params.addValue("ID", recno);
        params.addValue("EXTERNAL_ID", mrn);
        params.addValue("FIRST_NAME", truncate(name.getGivenAsSingleString(), 65));
        params.addValue("MIDDLE_NAME", null);
        params.addValue("LAST_NAME", truncate(name.getFamily(), 75));
        params.addValue("BIRTH_DATE", patient.getBirthDate());
        params.addValue("SEX_CD", truncate(patient.getGender().toCode(), 50));
        params.addValue("DECEASED_FLAG", deceased ? 1 : 0);
        params.addValue("LANGUAGE_CD", truncate(patient.getLanguage(), 50));
        params.addValue("RACE_CD", null);
        params.addValue("ETHNICITY_CD", null);
        params.addValue("MARITAL_STATUS_CD", truncate(patient.getMaritalStatus().getCodingFirstRep().getCode(), 50));
        params.addValue("RELIGION_CD", null);
        params.addValue("ZIP_CD", truncate(patient.getAddressFirstRep().getPostalCode(), 10));
        params.addValue("UPDATE_DATE", new Date());
        params.addValue("UPDATED_BY", "EMERSE-IT");
        params.addValue("CREATE_DATE", new Date());
        params.addValue("CREATED_BY", "EMERSE-IT");
        params.addValue("DELETED_FLAG", 0);

        try {
            jdbcTemplate.update(SQL, params);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    private String truncate(String value, int maxsize) {
        return value == null ? null : value.length() <= maxsize ? value : value.substring(0, maxsize);
    }

    private String getPatientInsertSQL() {
        return "INSERT INTO PATIENT (ID," + String.join(",", PATIENT_CREATE_FIELDS) + ") VALUES (PATIENT_SEQ.NEXTVAL,:"
                + String.join(",:", PATIENT_CREATE_FIELDS) + ")";
    }

    private String getPatientUpdateSQL() {
        StringBuilder sb = new StringBuilder();
        sb.append("UPDATE PATIENT SET ");
        String delim = "";

        for (String field: PATIENT_UPDATE_FIELDS) {
            sb.append(delim).append(field + "=:" + field);
            delim = ",";
        }

        sb.append(" WHERE ID=:ID");
        return sb.toString();
    }

    private Integer getPatientRecNum(String mrn) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("MRN", mrn);
        try {
            return jdbcTemplate.queryForObject("SELECT ID FROM PATIENT WHERE EXTERNAL_ID = :MRN", params, Integer.class);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public boolean authenticate(String username, String password) {
        try {
            MapSqlParameterSource params = new MapSqlParameterSource();
            params.addValue("USERNAME", username);
            String encodedPassword = jdbcTemplate.queryForObject(
                    "SELECT PASSWORD FROM LOGIN_ACCOUNT WHERE USER_ID = :USERNAME", params, String.class);
            return encodedPassword != null && passwordEncoder.matches(password, encodedPassword);
        } catch (Exception e) {
            log.error("Error while attempting user authentication.", e);
            return false;
        }
    }

    public void refreshQueue(RowMapper<?> rowMapper) {
        jdbcTemplate.query(QUEUE_CHECK, rowMapper);
    }
}

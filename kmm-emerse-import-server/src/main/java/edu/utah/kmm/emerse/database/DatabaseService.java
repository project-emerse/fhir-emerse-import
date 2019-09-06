package edu.utah.kmm.emerse.database;

import edu.utah.kmm.emerse.fhir.FhirClient;
import edu.utah.kmm.emerse.model.IdentifierType;
import edu.utah.kmm.emerse.security.Credentials;
import edu.utah.kmm.emerse.solr.IndexRequest;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hl7.fhir.dstu3.model.HumanName;
import org.hl7.fhir.dstu3.model.Patient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Date;
import java.util.List;

/**
 * Database-related services.
 */
public class DatabaseService {

    private static final Log log = LogFactory.getLog(DatabaseService.class);

    private static final String[] QUEUE_UPDATE_FIELDS = {
            "ID", "COMPLETED", "PROCESSED", "ERROR_TEXT", "PROCESSING_FLAG"
    };

    private static final String[] QUEUE_INSERT_FIELDS = {
            "SUBMITTED", "TOTAL", "PROCESSED", "PROCESSING_FLAG", "IDENTIFIER_TYPE", "IDENTIFIERS"
    };

    private static final String[] PATIENT_UPDATE_FIELDS = {
            "EXTERNAL_ID", "FIRST_NAME", "MIDDLE_NAME", "LAST_NAME", "BIRTH_DATE", "SEX_CD", "DECEASED_FLAG",
            "LANGUAGE_CD", "RACE_CD", "ETHNICITY_CD", "MARITAL_STATUS_CD", "RELIGION_CD", "ZIP_CD",
            "UPDATE_DATE", "UPDATED_BY"
    };

    private static final String[] PATIENT_INSERT_FIELDS = {
            "EXTERNAL_ID", "FIRST_NAME", "MIDDLE_NAME", "LAST_NAME", "BIRTH_DATE", "SEX_CD", "DECEASED_FLAG",
            "LANGUAGE_CD", "RACE_CD", "ETHNICITY_CD", "MARITAL_STATUS_CD", "RELIGION_CD", "ZIP_CD",
            "CREATE_DATE", "CREATED_BY", "DELETED_FLAG"
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

    private String getInsertSQL(String table, String[] insertFields, String sequence) {
        String idset = sequence == null ? ":ID" : sequence + ".NEXTVAL";
        return "INSERT INTO " + table + " (ID," + String.join(",", insertFields) + ") VALUES (" +
                idset + ",:" + String.join(",:", insertFields) + ")";
    }

    private String getUpdateSQL(String table, String[] updateFields) {
        StringBuilder sb = new StringBuilder();
        sb.append("UPDATE ").append(table).append(" SET ");
        String delim = "";

        for (String field : updateFields) {
            sb.append(delim).append(field + "=:" + field);
            delim = ",";
        }

        sb.append(" WHERE ID=:ID");
        return sb.toString();
    }

    private String getPatientInsertSQL() {
        return getInsertSQL("PATIENT", PATIENT_INSERT_FIELDS, "PATIENT_SEQ");
    }

    private String getPatientUpdateSQL() {
        return getUpdateSQL("PATIENT", PATIENT_INSERT_FIELDS);
    }

    /**
     * Creates or updates entry in EMERSE patient table.
     *
     * @param patient Patient resource.
     */
    public void createOrUpdatePatient(Patient patient, boolean canUpdate) {
        String mrn = fhirClient.extractMRN(patient);
        Integer recno = getPatientRecNum(mrn);

        if (recno != null && !canUpdate) {
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
        } catch (DataAccessException e) {
            throw new RuntimeException(e);
        }

    }

    private String truncate(String value, int maxsize) {
        return value == null ? null : value.length() <= maxsize ? value : value.substring(0, maxsize);
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

    private String getQueueInsertSQL() {
        return getInsertSQL("QUEUE", QUEUE_INSERT_FIELDS, null);
    }

    private String getQueueUpdateSQL() {
        return getUpdateSQL("QUEUE", QUEUE_UPDATE_FIELDS);
    }

    public IndexRequest queueRequest(List<String> ids, IdentifierType type) {
        return createOrUpdateIndexRequest(new IndexRequest(ids, type));
    }

    public IndexRequest createOrUpdateIndexRequest(IndexRequest request) {
        String SQL = request.initial() ? getQueueInsertSQL() : getQueueUpdateSQL();
        MapSqlParameterSource params = new MapSqlParameterSource();

        if (request.write(params)) {
            try {
                jdbcTemplate.update(SQL, params);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        return request;
    }

    public void refreshQueue(RowMapper<?> rowMapper) {
        jdbcTemplate.query(QUEUE_CHECK, rowMapper);
    }
}

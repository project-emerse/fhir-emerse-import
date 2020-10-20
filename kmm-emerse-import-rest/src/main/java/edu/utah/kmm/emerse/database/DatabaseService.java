package edu.utah.kmm.emerse.database;

import edu.utah.kmm.emerse.fhir.IdentifierType;
import edu.utah.kmm.emerse.patient.PatientDTO;
import edu.utah.kmm.emerse.patient.PatientService;
import edu.utah.kmm.emerse.security.Credentials;
import edu.utah.kmm.emerse.solr.IndexRequestDTO;
import edu.utah.kmm.emerse.solr.IndexRequestDTO.IndexRequestStatus;
import edu.utah.kmm.emerse.solr.SolrService;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hl7.fhir.dstu3.model.Patient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.*;

/**
 * Database-related services.
 */
public class DatabaseService {

    private static final Log log = LogFactory.getLog(DatabaseService.class);

    private static final String QUEUE_TABLE = "INDEXING_QUEUE";

    private static final String[] QUEUE_UPDATE_FIELDS = {
            "ID", "COMPLETED", "PROCESSED", "ERROR_TEXT", "STATUS", "ELAPSED"
    };

    private static final String[] QUEUE_INSERT_FIELDS = {
            "SUBMITTED", "TOTAL", "PROCESSED", "STATUS", "ELAPSED", "IDENTIFIER_TYPE", "IDENTIFIERS"
    };

    private static final String[] QUEUE_SUMMARY_FIELDS = {
            "ID", "COMPLETED", "SUBMITTED", "TOTAL", "PROCESSED", "STATUS", "ELAPSED", "IDENTIFIER_TYPE", "ERROR_TEXT"
    };

    private static final String QUEUE_FETCH_REQUEST = "SELECT * FROM " + QUEUE_TABLE + " WHERE ID=:ID";

    private static final String QUEUE_SCAN = "SELECT ID FROM " + QUEUE_TABLE + " WHERE COMPLETED IS NULL AND STATUS = 0 ORDER BY SUBMITTED ASC";

    private static final String QUEUE_DELETE_REQUEST = "DELETE FROM " + QUEUE_TABLE + " WHERE ID=:ID";

    private static final String PATIENT_TABLE = "PATIENT";

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

    private static final String ORACLE_VERSION = "SELECT BANNER FROM v$version WHERE BANNER LIKE 'Oracle%'";

    private final NamedParameterJdbcTemplate jdbcTemplate;

    private BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Autowired
    private SolrService solrService;

    @Autowired
    private PatientService patientService;

    public DatabaseService(
            DriverManagerDataSource dataSource,
            Credentials dbCredentials) {
        dataSource.setUsername(dbCredentials.getUsername());
        dataSource.setPassword(dbCredentials.getPassword());
        jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
    }

    public String getDatabaseVersion() {
        try {
            return jdbcTemplate.queryForObject(ORACLE_VERSION, Collections.emptyMap(), String.class);
        } catch (Exception e) {
            return "Unavailable";
        }
    }

    public boolean authenticate(
            String username,
            String password) {
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
        return getInsertSQL(PATIENT_TABLE, PATIENT_INSERT_FIELDS, "PATIENT_SEQ");
    }

    private String getPatientUpdateSQL() {
        return getUpdateSQL(PATIENT_TABLE, PATIENT_UPDATE_FIELDS);
    }

    /**
     * Creates or updates entry in EMERSE patient table.
     *
     * @param patient Patient resource.
     */
    public void createOrUpdatePatient(Patient patient, boolean canUpdate) {
        String mrn = patientService.extractMRN(patient);
        Integer recno = getPatientRecNum(mrn);

        if (recno != null && !canUpdate) {
            return;
        }

        String SQL = recno == null ? getPatientInsertSQL() : getPatientUpdateSQL();
        Map<String, Object> params = new HashMap<>();
        params.put("ID", recno);
        params.put("EXTERNAL_ID", mrn);
        params.put("UPDATE_DATE", new Date());
        params.put("UPDATED_BY", "EMERSE-IT");
        params.put("CREATE_DATE", new Date());
        params.put("CREATED_BY", "EMERSE-IT");
        params.put("DELETED_FLAG", 0);
        PatientDTO patientDTO = new PatientDTO(patient, params);

        try {
            jdbcTemplate.update(SQL, patientDTO.getMap());
            solrService.indexPatient(patientDTO);
        } catch (DataAccessException e) {
            throw new RuntimeException(e);
        }

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
        return getInsertSQL(QUEUE_TABLE, QUEUE_INSERT_FIELDS, null);
    }

    private String getQueueUpdateSQL() {
        return getUpdateSQL(QUEUE_TABLE, QUEUE_UPDATE_FIELDS);
    }

    public IndexRequestDTO fetchRequest(String id) {
        return jdbcTemplate.queryForObject(QUEUE_FETCH_REQUEST, Collections.singletonMap("ID", id), (rs, i) -> new IndexRequestDTO(rs));
    }

    public IndexRequestDTO queueRequest(
            List<String> ids,
            IdentifierType type) {
        return updateIndexRequest(new IndexRequestDTO(ids, type));
    }

    public IndexRequestDTO updateIndexRequest(IndexRequestDTO request) {
        if (request.changed()) {
            boolean delete = request.getStatus() == IndexRequestStatus.DELETED;
            String SQL = delete ? QUEUE_DELETE_REQUEST : request.initial() ? getQueueInsertSQL() : getQueueUpdateSQL();
            Map<String, Object> map = delete ? Collections.singletonMap("ID", request.getId()) : request.getMap();
            try {
                jdbcTemplate.update(SQL, map);
                request.clearChanged();
            } catch (DataAccessException e) {
                throw new RuntimeException(e);
            }
        }
        return request;
    }

    public void refreshQueue(RowMapper<?> rowMapper) {
        jdbcTemplate.query(QUEUE_SCAN, rowMapper);
    }

    public List<Map<String, Object>> fetchQueueEntries() {
        String sql = "SELECT " + StringUtils.join(QUEUE_SUMMARY_FIELDS, ",") + " FROM " + QUEUE_TABLE;
        return jdbcTemplate.queryForList(sql, Collections.emptyMap());
    }
}

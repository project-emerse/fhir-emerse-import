package edu.utah.kmm.emerse.database;

import edu.utah.kmm.emerse.patient.PatientDTO;
import edu.utah.kmm.emerse.patient.PatientService;
import edu.utah.kmm.emerse.solr.IndexRequestDTO;
import edu.utah.kmm.emerse.solr.IndexRequestDTO.IndexRequestStatus;
import edu.utah.kmm.emerse.solr.IndexRequestQueue;
import edu.utah.kmm.emerse.solr.SolrService;
import edu.utah.kmm.emerse.util.MiscUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hl7.fhir.dstu3.model.Patient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

/**
 * Database-related services.
 */
public class DatabaseService {

    private static final Log log = LogFactory.getLog(DatabaseService.class);

    private static final String QUEUE_TABLE = "INDEXING_QUEUE";

    private static final String[] QUEUE_UPDATE_FIELDS = {
            "ID", "SUBMITTED", "COMPLETED", "PROCESSED", "ERROR_TEXT", "STATUS", "ELAPSED"
    };

    private static final String[] QUEUE_INSERT_FIELDS = {
            "SERVER_ID", "SUBMITTED", "TOTAL", "PROCESSED", "STATUS", "ELAPSED", "IDENTIFIER_TYPE", "IDENTIFIERS"
    };

    private static final String[] QUEUE_SUMMARY_FIELDS = {
            "ID", "COMPLETED", "SUBMITTED", "TOTAL", "PROCESSED", "STATUS", "ELAPSED", "IDENTIFIER_TYPE", "ERROR_TEXT"
    };

    private static final String QUEUE_FETCH_REQUEST = "SELECT * FROM " + QUEUE_TABLE + " WHERE ID=:ID";

    private static final String QUEUE_SCAN = "SELECT ID FROM " + QUEUE_TABLE
            + " WHERE SERVER_ID=:SERVER_ID AND COMPLETED IS NULL AND STATUS = 0 ORDER BY SUBMITTED ASC";

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

    private static final String PATIENT_LIST_TABLE = "PATIENT_LIST";

    private static final String SOLR_INDEX_TABLE = "SOLR_INDEX";

    private static final String[] SOLR_UPDATE_FIELDS = {
            "START_DATETIME", "END_DATETIME", "PATIENT_COUNT"
    };

    private final NamedParameterJdbcTemplate jdbcTemplate;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Value("${server.uuid}")
    private String serverId;

    @Autowired
    private SolrService solrService;

    @Autowired
    private PatientService patientService;

    public DatabaseService(DataSource dataSource) {
        jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
    }

    public String getDatabaseVersion() {
        try {
            return getConnection().getMetaData().getDatabaseProductVersion();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Authenticate the user using EMERSE's LOGIN_ACCOUNT table.
     *
     * @param username The user name.
     * @param password The password.
     * @return True if authentication was successful.
     */
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

    /**
     * Returns the connection from the JDBC template.
     *
     * @return The database connection.
     */
    private Connection getConnection() {
        try {
            return jdbcTemplate.getJdbcTemplate().getDataSource().getConnection();
        } catch (SQLException e) {
            return MiscUtil.rethrow(e);
        }
    }

    /**
     * Returns the SQL to perform an insert operation.
     *
     * @param table The database table.
     * @param insertFields The fields to be inserted.
     * @param sequence If not null, the sequence table to use for the ID value.  Otherwise, the ID value must be
     *                 supplied when the insert operation is performed.
     * @return The SQL for the insert operation.
     */
    private String getInsertSQL(String table, String[] insertFields, String sequence) {
        String idset = sequence == null ? ":ID" : sequence + ".NEXTVAL";
        return "INSERT INTO " + table + " (ID," + String.join(",", insertFields) + ") VALUES (" +
                idset + ",:" + String.join(",:", insertFields) + ")";
    }

    /**
     * Returns the SQL to perform an update operation.
     *
     * @param table The database table.
     * @param updateFields The fields to be updated.
     * @return The SQL for the update operation.
     */
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

    /**
     * Returns the SQL to update the SOLR index table.
     */
    private String getSolrIndexUpdateSQL() {
        return getUpdateSQL(SOLR_INDEX_TABLE, SOLR_UPDATE_FIELDS);
    }

    /**
     * Updates the SOLR index table.
     *
     * @param start The start date.
     * @param end The end date.
     * @param patientCount The count of patients.
     */
    public void updateSolrIndexSummary(
            Date start,
            Date end,
            long patientCount) {
        String SQL = getSolrIndexUpdateSQL();
        Map<String, Object> params = new HashMap<>();
        params.put("ID", "documents");
        params.put("START_DATETIME", start == null ? new Date() : start);
        params.put("END_DATETIME", end == null ? new Date() : end);
        params.put("PATIENT_COUNT", patientCount);

        try {
            jdbcTemplate.update(SQL, params);
        } catch (DataAccessException e) {
            MiscUtil.rethrow(e);
        }

    }

    /**
     * Returns the SQL to insert a new patient in the PATIENT table.
     */
    private String getPatientInsertSQL() {
        return getInsertSQL(PATIENT_TABLE, PATIENT_INSERT_FIELDS, "PATIENT_SEQ");
    }

    /**
     * Returns the SQL to update an existing patient in the PATIENT table.
     */
    private String getPatientUpdateSQL() {
        return getUpdateSQL(PATIENT_TABLE, PATIENT_UPDATE_FIELDS);
    }

    /**
     * Creates or updates entry in the PATIENT table.
     *
     * @param patient Patient resource.
     */
    public void createOrUpdatePatient(Patient patient, boolean canUpdate) {
        String mrn = patientService.extractMRN(patient);
        Integer recno = getPatientEmerseId(mrn);

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

            if (recno == null) {
                recno = getPatientEmerseId(mrn);
                patientDTO.getMap().put("ID", recno);
            }

            solrService.indexPatient(patientDTO);
        } catch (DataAccessException e) {
            MiscUtil.rethrow(e);
        }

    }

    /**
     * Returns the EMERSE id for the patient with the specified MRN.
     *
     * @param mrn The patient's MRN.
     * @return The patient's EMERSE id, or null if the patient was not found.
     */
    private Integer getPatientEmerseId(String mrn) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("MRN", mrn);
        try {
            return jdbcTemplate.queryForObject("SELECT ID FROM PATIENT WHERE EXTERNAL_ID = :MRN", params, Integer.class);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    /**
     * Returns the SQL to perform an insert operation in the INDEXING_QUEUE table.
     */
    private String getQueueInsertSQL() {
        return getInsertSQL(QUEUE_TABLE, QUEUE_INSERT_FIELDS, null);
    }

    /**
     * Returns the SQL to perform an update operation in the INDEXING_QUEUE table.
     */
    private String getQueueUpdateSQL() {
        return getUpdateSQL(QUEUE_TABLE, QUEUE_UPDATE_FIELDS);
    }

    /**
     * Fetches an index request from the INDEXING_QUEUE table.
     *
     * @param id The index request ID.
     * @return The DTO for the index request.
     */
    public IndexRequestDTO fetchIndexRequest(String id) {
        return jdbcTemplate.queryForObject(QUEUE_FETCH_REQUEST, Collections.singletonMap("ID", id), (rs, i) -> new IndexRequestDTO(rs));
    }

    /**
     * Updates the INDEXING_QUEUE entry for an index request if that request has changed since the last update.
     *
     * @param request The index request to update.
     */
    public void updateIndexRequest(IndexRequestDTO request) {
        if (request.changed()) {
            boolean delete = request.getStatus() == IndexRequestStatus.DELETED;
            String SQL = delete ? QUEUE_DELETE_REQUEST : request.initial() ? getQueueInsertSQL() : getQueueUpdateSQL();
            Map<String, Object> map = delete ? Collections.singletonMap("ID", request.getId()) : request.getMap();
            try {
                jdbcTemplate.update(SQL, map);
                request.clearChanged();
            } catch (DataAccessException e) {
                log.error(e.getMessage(), e);
                MiscUtil.rethrow(e);
            }
        }
    }

    /**
     * Scans the INDEXING_QUEUE table for queued entries, populating the index request queue.
     *
     * @param queue The index request queue.
     */
    public void refreshQueue(IndexRequestQueue queue) {
        jdbcTemplate.query(QUEUE_SCAN, Collections.singletonMap("SERVER_ID", serverId), queue);
    }

    /**
     * Returns all INDEXING_QUEUE entries for this server id.
     */
    public List<Map<String, Object>> fetchQueueEntries() {
        String sql = "SELECT " + StringUtils.join(QUEUE_SUMMARY_FIELDS, ",")
                + " FROM " + QUEUE_TABLE
                + " WHERE SERVER_ID=:SERVER_ID"
                + " ORDER BY SUBMITTED DESC";
        return jdbcTemplate.queryForList(sql, Collections.singletonMap("SERVER_ID", serverId));
    }

    private void deleteAllRows(String table) {
        jdbcTemplate.update("DELETE FROM " + table, Collections.emptyMap());
    }

    public void deleteAllPatients() {
        deleteAllRows(PATIENT_LIST_TABLE);
        deleteAllRows(PATIENT_TABLE);
    }
}

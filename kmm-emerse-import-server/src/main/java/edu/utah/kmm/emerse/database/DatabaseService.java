package edu.utah.kmm.emerse.database;

import edu.utah.kmm.emerse.fhir.FhirService;
import edu.utah.kmm.emerse.security.Credentials;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hl7.fhir.dstu3.model.HumanName;
import org.hl7.fhir.dstu3.model.Patient;
import org.springframework.beans.factory.annotation.Autowired;
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

    private static final String[] PATIENT_FIELDS = {
            "EXTERNAL_ID","FIRST_NAME","MIDDLE_NAME","LAST_NAME","BIRTH_DATE","SEX_CD","DECEASED_FLAG",
            "LANGUAGE_CD","RACE_CD","ETHNICITY_CD","MARITAL_STATUS_CD","RELIGION_CD","ZIP_CD",
            "UPDATE_DATE","UPDATED_BY","CREATE_DATE","CREATED_BY"
    };

    private final NamedParameterJdbcTemplate jdbcTemplate;

    private final DriverManagerDataSource dataSource;

    private BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Autowired
    private FhirService fhirService;

    public DatabaseService(DriverManagerDataSource dataSource, Credentials credentials) {
        this.dataSource = dataSource;
        dataSource.setUsername(credentials.getUsername());
        dataSource.setPassword(credentials.getPassword());
        jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
    }

    /**
     * Creates or updates entry in EMERSE patient table.
     *
     * @param patient Patient resource.
     */
    public void createOrUpdatePatient(Patient patient, boolean update) {
        String mrn = fhirService.getMRN(patient);
        Integer recno = getPatientRecNum(mrn);

        if (recno != null && !update) {
            return;
        }

        String SQL = recno == null ? getPatientInsertSQL() : getPatientUpdateSQL();
        HumanName name = patient.getNameFirstRep();
        boolean deceased = patient.hasDeceasedDateTimeType() || (patient.hasDeceasedBooleanType() && patient.getDeceasedBooleanType().getValue());

        try {
            MapSqlParameterSource params = new MapSqlParameterSource();
            params.addValue("ID", recno);
            params.addValue("EXTERNAL_ID", mrn);
            params.addValue("FIRST_NAME", name.getGivenAsSingleString());
            params.addValue("MIDDLE_NAME", null);
            params.addValue("LAST_NAME", name.getFamily());
            params.addValue("BIRTH_DATE", patient.getBirthDate());
            params.addValue("SEX_CD", patient.getGender());
            params.addValue("DECEASED_FLAG", deceased ? 1 : 0);
            params.addValue("LANGUAGE_CD", patient.getLanguage());
            params.addValue("RACE_CD", null);
            params.addValue("ETHNICITY_CD", null);
            params.addValue("MARITAL_STATUS_CD", patient.getMaritalStatus().getCodingFirstRep().getCode());
            params.addValue("RELIGION_CD", null);
            params.addValue("ZIP_CD", patient.getAddressFirstRep().getPostalCode());
            params.addValue("UPDATE_DATE", new Date());
            params.addValue("UPDATED_BY", "EMERSE IMPORT TOOL");
            params.addValue("CREATE_DATE", new Date());
            params.addValue("CREATED_BY", "EMERSE IMPORT TOOL");
            jdbcTemplate.update(SQL, params);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    private String getPatientInsertSQL() {
        return "INSERT INTO PATIENT (ID," + String.join(",", PATIENT_FIELDS) + ") VALUES (PATIENT_SEQ.NEXTVAL,:"
                + String.join(",:", PATIENT_FIELDS) + ")";
    }

    private String getPatientUpdateSQL() {
        StringBuilder sb = new StringBuilder();
        sb.append("UPDATE PATIENT SET ");
        String delim = "";

        for (String field: PATIENT_FIELDS) {
            if (!field.startsWith("CREATE")) {
                sb.append(delim);
                sb.append(field + "=:" + field);
                delim = ",";
            }
        }

        sb.append(" WHERE ID=:ID");
        return sb.toString();
    }

    private Integer getPatientRecNum(String mrn) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("MRN", mrn);
        return jdbcTemplate.queryForObject("SELECT ID FROM PATIENT WHERE EXTERNAL_ID = :MRN", params, Integer.class);
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
}

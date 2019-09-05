package edu.utah.kmm.emerse.solr;

import edu.utah.kmm.emerse.model.IdentifierType;
import org.apache.commons.io.IOUtils;

import java.sql.ResultSet;
import java.util.Date;
import java.util.List;

public class IndexRequest {

    final long id;

    final Date submitted;

    final Date completed;

    final int totalPatients;

    final int processedPatients;

    final String errorText;

    final boolean processingFlag;

    final IdentifierType identifierType;

    final List<String> patientList;

    IndexRequest(ResultSet rs) {
        try {
            this.id = rs.getLong("ID");
            this.submitted = rs.getDate("SUBMITTED");
            this.completed = rs.getDate("COMPLETED");
            this.totalPatients = rs.getInt("TOTAL_PATIENTS");
            this.processedPatients = rs.getInt("PROCESSED_PATIENTS");
            this.errorText = rs.getString("ERROR_TEXT");
            this.processingFlag = rs.getInt("PROCESSING_FLAG") != 1;
            this.identifierType = toIdentifierType(rs.getString("IDENTIFIER_TYPE"));
            this.patientList = IOUtils.readLines(rs.getClob("PATIENT_LIST").getAsciiStream(), "UTF-8");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private IdentifierType toIdentifierType(String value) {
        try {
            return IdentifierType.valueOf(value);
        } catch (Exception e) {
            return null;
        }
    }
}

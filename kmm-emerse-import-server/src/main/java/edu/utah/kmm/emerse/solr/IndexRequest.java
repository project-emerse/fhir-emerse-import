package edu.utah.kmm.emerse.solr;

import org.apache.commons.io.IOUtils;

import java.sql.ResultSet;
import java.util.Date;
import java.util.List;

public class IndexRequest {

    public enum IdentifierType {
        MRN, // The medical record number
        ID   // The logical id.
    };

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
            this.identifierType = IdentifierType.valueOf(rs.getString("IDENTIFIER_TYPE"));
            this.patientList = IOUtils.readLines(rs.getClob("PATIENT_LIST").getAsciiStream(), "UTF-8");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

package edu.utah.kmm.emerse.solr;

import edu.utah.kmm.emerse.model.IdentifierType;
import org.apache.commons.io.IOUtils;

import java.sql.ResultSet;
import java.util.Date;
import java.util.List;

import static edu.utah.kmm.emerse.util.MiscUtil.toIdentifierType;

public class IndexRequest {

    final long id;

    final Date submitted;

    final Date completed;

    final int total;

    final int processed;

    final String errorText;

    final boolean processingFlag;

    final IdentifierType identifierType;

    final List<String> identifiers;

    IndexRequest(ResultSet rs) {
        try {
            this.id = rs.getLong("ID");
            this.submitted = rs.getDate("SUBMITTED");
            this.completed = rs.getDate("COMPLETED");
            this.total = rs.getInt("TOTAL");
            this.processed = rs.getInt("PROCESSED");
            this.errorText = rs.getString("ERROR_TEXT");
            this.processingFlag = rs.getInt("PROCESSING_FLAG") != 1;
            this.identifierType = toIdentifierType(rs.getString("IDENTIFIER_TYPE"));
            this.identifiers = IOUtils.readLines(rs.getClob("IDENTIFIERS").getAsciiStream(), "UTF-8");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}

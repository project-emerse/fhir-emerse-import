package edu.utah.kmm.emerse.solr;

import edu.utah.kmm.emerse.model.IdentifierType;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.util.Assert;

import java.io.Closeable;
import java.io.IOException;
import java.io.StringReader;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import static edu.utah.kmm.emerse.util.MiscUtil.toIdentifierType;

public class IndexRequest implements Iterable<String>, Closeable {

    final String id;

    final Date submitted;

    final int total;

    final IdentifierType identifierType;

    final List<String> identifiers;

    Date completed;

    int processed;

    boolean processingFlag;

    String errorText;

    private boolean changed;

    private boolean initial;

    public IndexRequest(Resource source) {
        try {
            this.identifiers = IOUtils.readLines(source.getInputStream(), "UTF-8");
            this.identifierType = toIdentifierType(identifiers.isEmpty() ? "" : identifiers.remove(0).trim());
            Assert.notNull(identifierType, "An valid identifier type was not found.");
            this.id = UUID.randomUUID().toString();
            this.submitted = now();
            this.total = identifiers.size();
            this.initial = true;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public IndexRequest(List<String> identifiers, IdentifierType identifierType) {
        this.id = UUID.randomUUID().toString();
        this.submitted = now();
        this.total = identifiers.size();
        this.identifiers = identifiers;
        this.identifierType = identifierType;
        this.initial = true;
    }

    IndexRequest(ResultSet rs) {
        try {
            id = rs.getString("ID");
            submitted = rs.getDate("SUBMITTED");
            completed = rs.getDate("COMPLETED");
            total = rs.getInt("TOTAL");
            processed = rs.getInt("PROCESSED");
            errorText = rs.getString("ERROR_TEXT");
            processingFlag = rs.getBoolean("PROCESSING_FLAG");
            identifierType = toIdentifierType(rs.getString("IDENTIFIER_TYPE"));
            identifiers = IOUtils.readLines(rs.getClob("IDENTIFIERS").getAsciiStream(), "UTF-8");
        } catch (IOException | SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void setErrorText(String errorText) {
        changed = true;
        this.errorText = errorText;
    }

    public void completed() {
        changed = true;
        completed = now();
    }

    public void processing(boolean active) {
        changed = true;
        processingFlag = active;
    }

    public boolean initial() {
        return initial;
    }

    public boolean write(MapSqlParameterSource params) {
        if (changed || initial) {
            params.addValue("ID", id);
            params.addValue("SUBMITTED", submitted);
            params.addValue("COMPLETED", completed);
            params.addValue("TOTAL", total);
            params.addValue("PROCESSED", processed);
            params.addValue("ERROR_TEXT", errorText);
            params.addValue("PROCESSING_FLAG", processingFlag);
            params.addValue("IDENTIFIER_TYPE", identifierType.name());
            params.addValue("IDENTIFIERS", initial ? new StringReader(StringUtils.join(identifiers, "\n")) : null);
            changed = initial = false;
            return true;
        }

        return false;
    }

    private Date now() {
        return new Date(System.currentTimeMillis());
    }

    @Override
    public Iterator<String> iterator() {
        return new Iterator<String>() {
            @Override
            public boolean hasNext() {
                processingFlag = processed < total;

                if (!processingFlag && completed == null) {
                    completed = now();
                }

                return processingFlag;
            }

            @Override
            public String next() {
                changed = true;
                return identifiers.get(processed++);
            }
        };
    }

    @Override
    public void close() throws IOException {
        processingFlag = false;
    }
}

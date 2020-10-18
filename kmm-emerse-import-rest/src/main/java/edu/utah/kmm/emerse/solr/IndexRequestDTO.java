package edu.utah.kmm.emerse.solr;

import edu.utah.kmm.emerse.database.BaseDTO;
import edu.utah.kmm.emerse.fhir.IdentifierType;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

import java.io.Closeable;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import static edu.utah.kmm.emerse.util.MiscUtil.toIdentifierType;

public class IndexRequestDTO extends BaseDTO implements Iterable<String>, Closeable {

    /**
     * Note: do not change the member order!
     */
    enum IndexRequestStatus {
        COMPLETED,
        QUEUED,
        RUNNING,
        SUSPENDED,
        ABORTED,
        ERROR
    }

    private enum FieldType {
        COMPLETED,
        ELAPSED,
        ERROR_TEXT,
        ID,
        IDENTIFIER_TYPE,
        IDENTIFIERS,
        PROCESSED,
        STATUS,
        SUBMITTED,
        TOTAL
    }

    private boolean changed;

    private boolean initial;

    private long started;

    private List<String> identifiers;

    private IdentifierType identifierType;

    public IndexRequestDTO(Resource source) {
        try {
            List<String> identifiers = IOUtils.readLines(source.getInputStream(), "UTF-8");
            IdentifierType identifierType = toIdentifierType(identifiers.isEmpty() ? "" : identifiers.remove(0).trim());
            init(identifiers, identifierType);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public IndexRequestDTO(List<String> identifiers, IdentifierType identifierType) {
        init(identifiers, identifierType);
    }

    public IndexRequestDTO(ResultSet rs) {
        put(FieldType.ID, rs, String.class);
        put(FieldType.SUBMITTED, rs, Date.class);
        put(FieldType.COMPLETED, rs, Date.class);
        put(FieldType.TOTAL, rs, Integer.class);
        put(FieldType.PROCESSED, rs, Integer.class);
        put(FieldType.STATUS, rs, Integer.class);
        put(FieldType.ELAPSED, rs, Integer.class);
        put(FieldType.ERROR_TEXT, rs, String.class);
        put(FieldType.IDENTIFIER_TYPE, rs, String.class);
        put(FieldType.IDENTIFIERS, rs, String.class);
        identifiers = stringToList(get(FieldType.IDENTIFIERS, String.class));
        identifierType = toIdentifierType(get(FieldType.IDENTIFIER_TYPE, String.class));
        changed = false;
    }

    private void init(List<String> identifiers, IdentifierType identifierType) {
        Assert.notNull(identifierType, "You must specify a valid identifier type");
        this.initial = true;
        this.identifiers = identifiers;
        this.identifierType = identifierType;
        put(FieldType.ID, UUID.randomUUID().toString());
        put(FieldType.SUBMITTED, now());
        put(FieldType.TOTAL, identifiers.size());
        put(FieldType.PROCESSED, 0);
        put(FieldType.STATUS, 0);
        put(FieldType.ELAPSED, 0);
        put(FieldType.ERROR_TEXT, null);
        put(FieldType.IDENTIFIER_TYPE, identifierType.name());
        put(FieldType.IDENTIFIERS, listToString(identifiers));
    }

    private List<String> stringToList(String value) {
        return Arrays.asList(value.split("\n"));
    }

    private String listToString(List<String> list) {
        return StringUtils.join(list, "\n");
    }

    private void put(FieldType type, ResultSet rs, Class<?> clazz) {
        try {
            put(type, rs.getObject(type.name(), clazz));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void put(FieldType field, Object value) {
        changed = true;
        map.put(field.name(), value);
    }

    private <T> T get(FieldType field, Class<T> clazz) {
        return (T) map.get(field.name());
    }

    private Date now() {
        return new Date();
    }

    public void setErrorText(String errorText) {
        put(FieldType.ERROR_TEXT, errorText);
        setStatus(IndexRequestStatus.ERROR);
    }

    public void start() {
        started = System.currentTimeMillis();
        setStatus(IndexRequestStatus.RUNNING);
    }

    public void completed() {
        put(FieldType.COMPLETED, now());
        setStatus(IndexRequestStatus.COMPLETED);
    }

    public IndexRequestStatus getStatus() {
        Integer status = get(FieldType.STATUS, Integer.class);
        return status == null ? null : IndexRequestStatus.values()[status];
    }

    public void setStatus(IndexRequestStatus status) {
        put(FieldType.STATUS, status.ordinal());
    }

    public boolean initial() {
        return initial;
    }

    public boolean changed() {
        return changed;
    }

    public void clearChanged() {
        this.changed = false;
        this.initial = false;
    }

    public IdentifierType getIdentifierType() {
        return identifierType;
    }

    @Override
    public Iterator<String> iterator() {
        return new Iterator<String>() {
            int total = get(FieldType.TOTAL, Integer.class);
            int processed = get(FieldType.PROCESSED, Integer.class);
            boolean processingFlag;

            @Override
            public boolean hasNext() {
                processingFlag = processed < total;

                if (!processingFlag) {
                    completed();
                }

                return processingFlag;
            }

            @Override
            public String next() {
                put(FieldType.PROCESSED, processed + 1);
                return identifiers.get(processed++);
            }
        };
    }

    @Override
    public void close() {
        if (getStatus() == IndexRequestStatus.RUNNING) {
            setStatus(IndexRequestStatus.SUSPENDED);
        }

        if (started > 0) {
            int elapsed = (int) (System.currentTimeMillis() - started);
            Integer current = get(FieldType.ELAPSED, Integer.class);
            elapsed += current == null ? 0 : current.intValue();
            put(FieldType.ELAPSED, elapsed);
            started = 0;
        }
    }
}

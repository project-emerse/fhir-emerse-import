package edu.utah.kmm.emerse.solr;

import edu.utah.kmm.emerse.database.BaseDTO;
import edu.utah.kmm.emerse.fhir.IdentifierType;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.Resource;

import java.io.Closeable;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import static edu.utah.kmm.emerse.util.MiscUtil.toIdentifierType;

public class IndexRequestDTO extends BaseDTO implements Closeable {

    public interface ICloseCallback {
        void onClose(IndexRequestDTO request);
    }

    /**
     * Note: do not change the member order!
     */
    public enum IndexRequestStatus {
        QUEUED,
        RUNNING,
        SUSPENDED,
        COMPLETED,
        ABORTED,
        ERROR,
        DELETED
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

    private final List<ICloseCallback> closeCallbacks = new ArrayList<>();

    private boolean changed;

    private boolean initial;

    private long started;

    private final List<String> identifiers;

    private final IdentifierType identifierType;

    IndexRequestDTO(Resource source) {
        try {
            this.identifiers = IOUtils.readLines(source.getInputStream(), "UTF-8");
            this.identifierType = toIdentifierType(identifiers.isEmpty() ? "" : identifiers.remove(0).trim());
            this.initial = true;
            put(FieldType.ID, UUID.randomUUID().toString());
            put(FieldType.SUBMITTED, now());
            put(FieldType.TOTAL, identifiers.size());
            put(FieldType.PROCESSED, 0);
            put(FieldType.STATUS, 0);
            put(FieldType.ELAPSED, 0);
            put(FieldType.ERROR_TEXT, null);
            put(FieldType.IDENTIFIER_TYPE, identifierType.name());
            put(FieldType.IDENTIFIERS, listToString(identifiers));
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
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
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private void put(
            FieldType field,
            Object value) {
        String name = field.name();

        if (!map.containsKey(name) || !Objects.equals(value, map.get(name))) {
            changed = true;
            map.put(name, value);
        }
    }

    private <T> T get(
            FieldType field,
            Class<T> clazz) {
        return (T) map.get(field.name());
    }

    private Date now() {
        return new Date();
    }

    public int getProcessed() {
        return get(FieldType.PROCESSED, Integer.class);
    }

    public void processed() {
        put(FieldType.PROCESSED, getProcessed() + 1);
    }

    public String getId() {
        return get(FieldType.ID, String.class);
    }

    public List<String> getIdentifiers(boolean unprocessed) {
        List<String> list = unprocessed
                ? identifiers.subList(getProcessed(), identifiers.size())
                : identifiers;
        return Collections.unmodifiableList(list);
    }

    public void error(String errorText) {
        errorText = StringUtils.truncate(StringUtils.trimToNull(errorText), 200);
        put(FieldType.ERROR_TEXT, errorText);

        if (errorText != null) {
            setStatus(IndexRequestStatus.ERROR);
        }
    }

    public IndexRequestDTO start() {
        started = System.currentTimeMillis();
        error(null);
        put(FieldType.COMPLETED, null);
        setStatus(IndexRequestStatus.RUNNING);
        return this;
    }

    public boolean completed() {
        return hasStatus(IndexRequestStatus.RUNNING) && stop(IndexRequestStatus.COMPLETED);
    }

    public boolean abort() {
        return !hasStatus(IndexRequestStatus.COMPLETED)
                && stop(IndexRequestStatus.ABORTED);
    }

    public boolean suspend() {
        return hasStatus(IndexRequestStatus.RUNNING, IndexRequestStatus.QUEUED)
                && stop(IndexRequestStatus.SUSPENDED);
    }

    public boolean resume() {
        return !hasStatus(IndexRequestStatus.RUNNING, IndexRequestStatus.COMPLETED)
                && requeue(false);
   }

    public boolean restart() {
        return !hasStatus(IndexRequestStatus.RUNNING)
                && requeue(true);
    }

    private boolean requeue(boolean resetCount) {
        error(null);
        setStatus(IndexRequestStatus.QUEUED);
        put(FieldType.COMPLETED, null);
        put(FieldType.SUBMITTED, now());

        if (resetCount) {
            put(FieldType.PROCESSED, 0);
            put(FieldType.ELAPSED, 0);
        }

        return true;
    }

    public boolean delete() {
        return stop(IndexRequestStatus.DELETED);
    }

    private boolean stop(IndexRequestStatus status) {
        put(FieldType.COMPLETED, status == IndexRequestStatus.SUSPENDED ? null : now());
        setStatus(status);
        close();
        return true;
    }

    public IndexRequestStatus getStatus() {
        Integer status = get(FieldType.STATUS, Integer.class);
        return status == null ? null : IndexRequestStatus.values()[status];
    }

    public boolean hasStatus(IndexRequestStatus... statuses) {
        return Arrays.stream(statuses)
                .filter(status -> getStatus() == status)
                .findFirst()
                .map(status -> true)
                .orElse(false);
    }

    private void setStatus(IndexRequestStatus status) {
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

    public void registerCloseCallback(ICloseCallback callback) {
        closeCallbacks.add(callback);
    }

    @Override
    public void close() {
        if (getStatus() == IndexRequestStatus.RUNNING) {
            setStatus(IndexRequestStatus.SUSPENDED);
        }

        if (started > 0) {
            int elapsed = (int) (System.currentTimeMillis() - started);
            Integer current = get(FieldType.ELAPSED, Integer.class);
            elapsed += current == null ? 0 : current;
            put(FieldType.ELAPSED, elapsed);
            started = 0;
        }

        closeCallbacks.forEach(cb -> {
            try {
                cb.onClose(this);
            } catch (Exception e) {
                // NOP
            }
        });

        closeCallbacks.clear();
    }
}

package edu.utah.kmm.emerse.solr;

import edu.utah.kmm.emerse.database.BaseDTO;
import edu.utah.kmm.emerse.fhir.IdentifierType;
import edu.utah.kmm.emerse.util.MiscUtil;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

import java.io.Closeable;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import static edu.utah.kmm.emerse.util.MiscUtil.toIdentifierType;

/**
 * DTO representing an index request.
 */
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
        TOTAL,
        SERVER_ID
    }

    private final List<ICloseCallback> closeCallbacks = new ArrayList<>();

    private boolean changed;

    private boolean initial;

    private boolean closed;

    private long started;

    private final List<String> identifiers;

    private final IdentifierType identifierType;

    IndexRequestDTO(Resource source, String serverId) {
        try {
            this.identifiers = IOUtils.readLines(source.getInputStream(), "UTF-8");
            this.identifierType = toIdentifierType(identifiers.isEmpty() ? "" : identifiers.remove(0).trim());
            this.initial = true;
            put(FieldType.ID, UUID.randomUUID().toString());
            put(FieldType.SERVER_ID, serverId);
            put(FieldType.SUBMITTED, now());
            put(FieldType.TOTAL, identifiers.size());
            put(FieldType.PROCESSED, 0);
            put(FieldType.STATUS, 0);
            put(FieldType.ELAPSED, 0);
            put(FieldType.ERROR_TEXT, null);
            put(FieldType.IDENTIFIER_TYPE, identifierType.name());
            put(FieldType.IDENTIFIERS, listToString(identifiers));
        } catch (IOException e) {
            throw MiscUtil.toUnchecked(e);
        }
    }

    public IndexRequestDTO(ResultSet rs) {
        put(FieldType.ID, rs, String.class);
        put(FieldType.SERVER_ID, rs, String.class);
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

    /**
     * Converts a newline-delimited string to a list.
     *
     * @param value The string to convert.
     * @return The list of delimited values (never null).
     */
    private List<String> stringToList(String value) {
        return value == null ? Collections.emptyList() : Arrays.asList(value.split("\n"));
    }

    /**
     * Converts a list to a newline-delimited string.
     *
     * @param list The list to convert.
     * @return The newline-delimited string.
     */
    private String listToString(List<String> list) {
        return StringUtils.join(list, "\n");
    }

    /**
     * Extracts a field from a result set and stores it in the DTO map.
     *
     * @param type The field type to extract.
     * @param rs The result set.
     * @param clazz The data type of the field.
     */
    private void put(FieldType type, ResultSet rs, Class<?> clazz) {
        try {
            put(type, rs.getObject(type.name(), clazz));
        } catch (SQLException e) {
            MiscUtil.rethrow(e);
        }
    }

    /**
     * Puts a field value into the DTO map.  Updates the changed status as appropriate.
     *
     * @param field The field type.
     * @param value The value to store.
     */
    private void put(
            FieldType field,
            Object value) {
        String name = field.name();

        if (!map.containsKey(name) || !Objects.equals(value, map.get(name))) {
            changed = true;
            map.put(name, value);
        }
    }

    /**
     * Returns a field value from the DTO map.
     *
     * @param field The field type.
     * @param clazz The expected data type.
     * @param <T> The expected data type.
     * @return The field's value.
     */
    private <T> T get(
            FieldType field,
            Class<T> clazz) {
        return (T) map.get(field.name());
    }

    /**
     * Returns today's date/time.
     */
    private Date now() {
        return new Date();
    }

    /**
     * Returns the number of processed entries.
     */
    public int getProcessed() {
        return get(FieldType.PROCESSED, Integer.class);
    }

    /**
     * Increments the number of processed entries.
     */
    public void processed() {
        put(FieldType.PROCESSED, getProcessed() + 1);
    }

    /**
     * Returns the unique ID for this request.
     */
    public String getId() {
        return get(FieldType.ID, String.class);
    }

    /**
     * Returns the list of identifiers for this request.
     *
     * @param unprocessed If true, return only unprocessed entries.
     * @return The list of identifiers.
     */
    public List<String> getIdentifiers(boolean unprocessed) {
        List<String> list = unprocessed
                ? identifiers.subList(getProcessed(), identifiers.size())
                : identifiers;
        return Collections.unmodifiableList(list);
    }

    /**
     * Sets the error text for this request.
     *
     * @param errorText The error text.
     */
    public void error(String errorText) {
        errorText = StringUtils.truncate(StringUtils.trimToNull(errorText), 200);
        put(FieldType.ERROR_TEXT, errorText);

        if (errorText != null) {
            setStatus(IndexRequestStatus.ERROR);
        }
    }

    /**
     * Sets the request state to started.
     *
     * @return This request (for chaining).
     */
    public IndexRequestDTO start() {
        assertNotClosed();
        started = System.currentTimeMillis();
        error(null);
        put(FieldType.COMPLETED, null);
        setStatus(IndexRequestStatus.RUNNING);
        return this;
    }

    /**
     * Sets the request state to completed.
     *
     * @return True if the operation was successfully performed.
     */
    public boolean completed() {
        return hasStatus(IndexRequestStatus.RUNNING) && stop(IndexRequestStatus.COMPLETED);
    }

    /**
     * Sets the request state to aborted.
     *
     * @return True if the operation was successfully performed.
     */
    public boolean abort() {
        return !hasStatus(IndexRequestStatus.COMPLETED)
                && stop(IndexRequestStatus.ABORTED);
    }

    /**
     * Sets the request state to suspended.
     *
     * @return True if the operation was successfully performed.
     */
    public boolean suspend() {
        return hasStatus(IndexRequestStatus.RUNNING, IndexRequestStatus.QUEUED)
                && stop(IndexRequestStatus.SUSPENDED);
    }

    /**
     * Sets the request state to resumed.
     *
     * @return True if the operation was successfully performed.
     */
    public boolean resume() {
        return !hasStatus(IndexRequestStatus.RUNNING, IndexRequestStatus.COMPLETED)
                && requeue(false);
   }

    /**
     * Sets the request state to restarted.
     *
     * @return True if the operation was successfully performed.
     */
    public boolean restart() {
        return !hasStatus(IndexRequestStatus.RUNNING)
                && requeue(true);
    }

    /**
     * Performs the specified action on this request.
     *
     * @return True if the operation was successfully performed.
     */
    public boolean performAction(IndexRequestAction.Action action) {
        switch (action) {
            case ABORT:
                return abort();
            case RESUME:
                return resume();
            case SUSPEND:
                return suspend();
            case DELETE:
                return delete();
            case RESTART:
                return restart();
            default:
                return false;
        }
    }

    /**
     * Requeues this request for execution.
     *
     * @param resetCount If true, reset the processed and elapsed values.
     * @return Always true.
     */
    private boolean requeue(boolean resetCount) {
        assertNotClosed();
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

    /**
     * Deletes this request from the database.
     *
     * @return Always true.
     */
    public boolean delete() {
        return stop(IndexRequestStatus.DELETED);
    }

    /**
     * Updates the status for this request and then closes it.
     *
     * @param status The new request status.
     * @return Always true.
     */
    private boolean stop(IndexRequestStatus status) {
        assertNotClosed();
        put(FieldType.COMPLETED, status == IndexRequestStatus.SUSPENDED ? null : now());
        setStatus(status);
        close();
        return true;
    }

    /**
     * Returns the status of this request.
     */
    public IndexRequestStatus getStatus() {
        Integer status = get(FieldType.STATUS, Integer.class);
        return status == null ? null : IndexRequestStatus.values()[status];
    }

    /**
     * Returns true if this request's status is one of the specified types.
     *
     * @param statuses List of statuses to check.
     * @return True if this request's status is one of the specified types.
     */
    public boolean hasStatus(IndexRequestStatus... statuses) {
        return Arrays.stream(statuses)
                .filter(status -> getStatus() == status)
                .findFirst()
                .map(status -> true)
                .orElse(false);
    }

    /**
     * Updates the request's status.
     *
     * @param status The new status.
     */
    private void setStatus(IndexRequestStatus status) {
        put(FieldType.STATUS, status.ordinal());
    }

    /**
     * Returns true if the request has not yet been persisted.
     */
    public boolean initial() {
        return initial;
    }

    /**
     * Returns true if the state of this request has changed.
     */
    public boolean changed() {
        return changed;
    }

    /**
     * Clears the changed flag.
     */
    public void clearChanged() {
        this.changed = false;
        this.initial = false;
    }

    /**
     * Returns the identifier type for this request.
     */
    public IdentifierType getIdentifierType() {
        return identifierType;
    }

    /**
     * Registers a close callback.
     *
     * @param callback The callback to be executed when this request is closed.
     */
    public void registerCloseCallback(ICloseCallback callback) {
        closeCallbacks.add(callback);
    }

    /**
     * Asserts that the request is not closed, throwing an exception if it is.
     */
    private void assertNotClosed() {
        Assert.state(!closed, () -> "Index request '" + getId() + "' has already been closed");
    }

    /**
     * Close the DTO, invoking all registered close callbacks.
     */
    @Override
    public void close() {
        if (closed) {
            return;
        }

        closed = true;

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

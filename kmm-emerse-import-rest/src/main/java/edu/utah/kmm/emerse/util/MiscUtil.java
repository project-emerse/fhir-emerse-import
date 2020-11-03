package edu.utah.kmm.emerse.util;

import edu.utah.kmm.emerse.fhir.IdentifierType;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;

import java.text.SimpleDateFormat;

/**
 * Miscellaneous utility methods.
 */
public class MiscUtil {

    public static final SimpleDateFormat dateTimeParser = new SimpleDateFormat("yyyy-MM-dd hh:mm a");

    /**
     * Converts a string value to an identifier type.
     *
     * @param value The input value.
     * @return The identifier type.
     * @throws IllegalArgumentException if the input value is invalid.
     */
    public static IdentifierType toIdentifierType(String value) {
        try {
            return IdentifierType.valueOf(value.toUpperCase());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid or missing identifier type: " + value);
        }
    }

    /**
     * Asserts that the identifier type is one of the allowed values.
     *
     * @param value The identifier type to test.
     * @param allowed A list of allowed identifier types.
     * @throws IllegalArgumentException If the specified identifier type is not one of the allowed types.
     */
    public static void validateIdentiferType(
            IdentifierType value,
            IdentifierType... allowed) {
        Assert.isTrue(ArrayUtils.contains(allowed, value), () ->
                "Illegal identifier type " + value + "; must be one of " + StringUtils.join(allowed, ", "));
    }

    /**
     * Converts a checked exception to an unchecked one.
     *
     * @param e The exception to convert.
     * @return An unchecked exception.
     */
    public static RuntimeException toUnchecked(Exception e) {
        return e instanceof RuntimeException ? (RuntimeException) e : new RuntimeException(e.getMessage(), e);
    }

    /**
     * Rethrows a checked exception as an unchecked one.
     *
     * @param e The checked exception.
     * @param <T> The dummy return type.
     * @return Never actually returns a value (simplifies use in a method that provides a return value).
     */
    public static <T> T rethrow(Exception e) {
        throw toUnchecked(e);
    }

    private MiscUtil() {
    }

}

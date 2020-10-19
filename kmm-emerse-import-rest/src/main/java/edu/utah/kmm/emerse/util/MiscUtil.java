package edu.utah.kmm.emerse.util;

import edu.utah.kmm.emerse.fhir.IdentifierType;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;

import java.text.SimpleDateFormat;

public class MiscUtil {

    public static final SimpleDateFormat dateParser = new SimpleDateFormat("yyyy-MM-dd");

    public static final SimpleDateFormat dateTimeParser = new SimpleDateFormat("yyyy-MM-dd hh:mm a");

    public static IdentifierType toIdentifierType(String value) {
        return IdentifierType.valueOf(value.toUpperCase());
    }

    public static void validateIdentiferType(IdentifierType value, IdentifierType... allowed) {
        Assert.isTrue(ArrayUtils.contains(allowed, value), () ->
                "Illegal identifier type " + value + "; must be one of " + StringUtils.join(allowed, ", "));
    }

    private MiscUtil() {}
}

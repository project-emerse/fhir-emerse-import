package edu.utah.kmm.emerse.util;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.charfilter.HTMLStripCharFilter;

import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;

/**
 * HTML utility methods.
 */
public class HTMLUtil {

    private static final Log log = LogFactory.getLog(HTMLUtil.class);

    /**
     * Strip HTML tags from a string value.
     *
     * @param value
     * @return
     */
    public static String stripTags(String value) {
        return value == null ? null : readerToStr(new StringReader(value), true);
    }

    /**
     * Convert a character reader to a string, optionally stripping HTML tags.
     *
     * @param reader A character-based reader.
     * @param stripHTML If true, any encountered HTML tags are removed from the output.
     * @return A string representation of the input.
     */
    public static String readerToStr(Reader reader, boolean stripHTML) {
        try {
            if (reader == null) {
                return null;
            }

            if (stripHTML) {
                reader = new HTMLStripCharFilter(reader);
            }

            StringWriter writer = new StringWriter(1024);
            IOUtils.copy(reader, writer);
            return writer.toString();
        } catch (Exception e) {
            log.warn("Exception while converting text data", e);
            return null;
        }
    }

    private HTMLUtil() {}
}

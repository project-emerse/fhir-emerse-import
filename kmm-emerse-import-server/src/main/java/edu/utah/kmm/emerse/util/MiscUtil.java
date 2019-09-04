package edu.utah.kmm.emerse.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;

public class MiscUtil {

    public static final SimpleDateFormat dateParser = new SimpleDateFormat("yyyy-MM-dd");

    public static final SimpleDateFormat dateTimeParser = new SimpleDateFormat("yyyy-MM-dd hh:mm a");

    public static URI toURI(String uri) {
        try {
            return new URI(uri);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private MiscUtil() {}
}

package edu.utah.kmm.emerse.util;

import java.net.URI;
import java.net.URISyntaxException;

public class MiscUtil {

    public static URI toURI(String uri) {
        try {
            return new URI(uri);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private MiscUtil() {}
}

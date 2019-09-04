package edu.utah.kmm.emerse.model;

import org.apache.commons.codec.binary.Base64;
import org.apache.solr.common.StringUtils;

public class DocumentContent {

    private static final Base64 decoder = new Base64();

    private final String content;

    private final String contentType;

    public DocumentContent(String content, String contentType) {
        this.content = new String(decoder.decode(content));
        this.contentType = StringUtils.isEmpty(contentType) ? "text/html" : contentType;
    }

    public DocumentContent(byte[] content, String contentType) {
        this(new String(content), contentType);
    }

    public String getContentType() {
        return contentType;
    }

    public String getContent() {
        return content;
    }
}

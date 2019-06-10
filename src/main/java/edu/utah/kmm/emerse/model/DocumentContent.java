package edu.utah.kmm.emerse.model;

import org.apache.commons.codec.binary.Base64;

public class DocumentContent {

    public final String content;

    public final String contentType;

    public DocumentContent(String content, String contentType) {
        this.content = content;
        this.contentType = contentType;
    }

    public DocumentContent(byte[] content, String contentType) {
        this(new String(Base64.decodeBase64(content)), contentType);
    }
}

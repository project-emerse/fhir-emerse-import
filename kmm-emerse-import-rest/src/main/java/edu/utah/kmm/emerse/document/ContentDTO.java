package edu.utah.kmm.emerse.document;

import edu.utah.kmm.emerse.database.BaseDTO;
import org.apache.commons.codec.binary.Base64;
import org.apache.solr.common.StringUtils;

/**
 * DTO encapsulating document content and content type.
 */
public class ContentDTO extends BaseDTO {

    private static final Base64 decoder = new Base64();

    private final String content;

    private final String contentType;

    public ContentDTO(String base64Content, String contentType) {
        super();
        try {
            content = base64Content == null ? null : new String((byte[]) decoder.decode(base64Content));
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        map.put("RPT_TEXT", content);
        this.contentType = StringUtils.isEmpty(contentType) ? "text/plain" : contentType;
    }

    public ContentDTO(byte[] base64Content, String contentType) {
        this(new String(base64Content), contentType);
    }

    public boolean isEmpty() {
        return content == null || content.isEmpty();
    }

    public String getContent() {
        return content;
    }

    public String getContentType() {
        return contentType;
    }
}

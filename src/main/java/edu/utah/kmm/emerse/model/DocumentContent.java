package edu.utah.kmm.emerse.model;

import edu.utah.kmm.emerse.util.HTMLUtil;
import edu.utah.kmm.emerse.util.XMLUtil;
import org.apache.commons.codec.binary.Base64;

public class DocumentContent {

    private final String content;

    private final String contentType;

    public DocumentContent(String content, String contentType) {
        this.content = content;
        this.contentType = contentType;
    }

    public DocumentContent(byte[] content, String contentType) {
        this(new String(Base64.decodeBase64(content)), contentType);
    }

    public String getContent() {
        if ("application/xml".equals(contentType)) {
            return HTMLUtil.stripTags(XMLUtil.transformXml(content, "cds2html.xsl"));
        }

        if ("text/html".equals(contentType)) {
            return HTMLUtil.stripTags(content);
        }

        return content;
    }
}

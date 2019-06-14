package edu.utah.kmm.emerse.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.InputStream;

/**
 * XML Utilities
 */
public class XMLUtil {

    private static final Log log = LogFactory.getLog(XMLUtil.class);

    public static final DocumentBuilderFactory DOCUMENT_BUILDER_FACTORY = DocumentBuilderFactory.newInstance();

    public static String transformXml(String xml, String xslt) {
        try {
            Document doc = DOCUMENT_BUILDER_FACTORY.newDocumentBuilder().parse(xml);
            InputStream is = XMLUtil.class.getResourceAsStream("/" + xslt);
            StreamSource source = new StreamSource(is);
            Transformer transformer = TransformerFactory.newInstance().newTransformer(source);
            StreamResult result = new StreamResult();
            transformer.transform(new DOMSource(doc), result);
            return xml;
        } catch (Exception e) {
            log.error("Error parsing XML", e);
            return xml;
        }
    }

    private XMLUtil() {}
}

package edu.utah.kmm.emerse;

import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;

import ca.uhn.fhir.model.api.IResource;
import ca.uhn.fhir.model.dstu2.composite.AttachmentDt;
import ca.uhn.fhir.model.dstu2.resource.Binary;
import ca.uhn.fhir.model.dstu2.resource.Bundle;
import ca.uhn.fhir.model.dstu2.resource.DocumentReference;
import ca.uhn.fhir.model.dstu2.resource.Patient;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.apache.commons.codec.binary.Base64;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.common.SolrInputDocument;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.charfilter.HTMLStripCharFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

/**
 * Pulls documents as FHIR resources and presents them for indexing.
 */
public class DocumentIndexer {

    private static final Log log = LogFactory.getLog(DocumentIndexer.class);

    public static final DocumentBuilderFactory DOCUMENT_BUILDER_FACTORY = DocumentBuilderFactory.newInstance();

    @Autowired
    private IGenericClient fhirClient;

    @Autowired
    private HttpSolrClient solrClient;

    public void indexDocuments(Patient patient) {
    	Bundle bundle = fhirClient.search()
                .forResource(DocumentReference.class)
                .where(DocumentReference.SUBJECT.hasId(patient.getId()))
                .returnBundle(Bundle.class)
                .execute();

    	for (Bundle.Entry entry: bundle.getEntry()) {
            IResource resource = entry.getResource();

            if (resource instanceof DocumentReference) {
                processDocumentReference(patient, (DocumentReference) resource);
            }
        }
    }

    private void processDocumentReference(Patient patient, DocumentReference documentReference) {
        if (!documentReference.getContent().isEmpty()) {
            DocumentReference.Content content = documentReference.getContentFirstRep();
            AttachmentDt attachment = content.getAttachment();

            if (!attachment.getDataElement().isEmpty()) {
                String contentType = attachment.getContentType();
                indexDocument(patient, documentReference, attachment.getData(), contentType);
                return;
            }

            if (!attachment.getUrlElement().isEmpty()) {
                Binary data = fhirClient.read().resource(Binary.class).withUrl(attachment.getUrl()).execute();
                indexDocument(patient, documentReference, data.getContent(), data.getContentType());
                return;
            }
        }
     }

    private void indexDocument(Patient patient, DocumentReference documentReference, byte[] content, String contentType) {
        String body = new String(Base64.decodeBase64(content));
        SolrInputDocument document = new SolrInputDocument();
        document.addField("RPT_ID", documentReference.getId());
        document.addField("MRN", patient.getId()); // TODO: Need MRN here
        document.addField("RPT_DATE", documentReference.getCreated());
        document.addField("SOURCE", "source1");
        document.addField("RPT_TEXT", stripTags(body));
        try {
            solrClient.add(document);
            solrClient.commit();
        } catch (Exception e) {
            log.error("Error indexing document", e);
        }
    }

    private String processContent(String body, String contentType) {
        if ("text/html".equals(contentType)) {
            return stripTags(body);
        }

        if ("application/xml".equals(contentType)) {
            return transformXml(body);
        }

        return body;
    }

    private String transformXml(String body) {
        try {
            Document doc = DOCUMENT_BUILDER_FACTORY.newDocumentBuilder().parse(body);
            InputStream is = DocumentIndexer.class.getResourceAsStream("/cds2html.xsl");
            StreamSource source = new StreamSource(is);
            Transformer transformer = TransformerFactory.newInstance().newTransformer(source);
            StreamResult result = new StreamResult();
            transformer.transform(new DOMSource(doc), result);
            return body;
        } catch (Exception e) {
            log.error("Error parsing document body", e);
            return body;
        }
    }

    /**
     * Strip HTML tags from a string value.
     * 
     * @param value
     * @return
     */
    protected String stripTags(String value) {
        return value == null ? null : readerToStr(new StringReader(value), true);
    }
    
    /**
     * Convert a character reader to a string, optionally stripping HTML tags.
     * 
     * @param reader A character-based reader.
     * @param stripHTML If true, any encountered HTML tags are removed from the output.
     * @return A string representation of the input.
     */
    protected String readerToStr(Reader reader, boolean stripHTML) {
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
    
}

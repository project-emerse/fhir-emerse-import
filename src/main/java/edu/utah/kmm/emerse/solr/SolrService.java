package edu.utah.kmm.emerse.solr;

import ca.uhn.fhir.model.dstu2.resource.DocumentReference;
import ca.uhn.fhir.model.dstu2.resource.Patient;
import edu.utah.kmm.emerse.fhir.FhirService;
import edu.utah.kmm.emerse.model.DocumentContent;
import edu.utah.kmm.emerse.util.HTMLUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.impl.XMLResponseParser;
import org.apache.solr.common.SolrInputDocument;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * Solr-related services.
 */
public class SolrService {

    private static final Log log = LogFactory.getLog(SolrService.class);

    private final HttpSolrClient solrClient;

    @Autowired
    private FhirService fhirService;

    public SolrService(String baseSolrUrl) {
        solrClient = new HttpSolrClient.Builder(baseSolrUrl)
                .withResponseParser(new XMLResponseParser())
                .allowCompression(true)
                .build();
    }

    public void indexDocuments(Patient patient) {
        List<DocumentReference> documents = fhirService.getDocuments(patient);

        for (DocumentReference document: documents) {
            indexDocument(patient, document);
         }
    }

    public void indexDocument(Patient patient, DocumentReference documentReference) {
        DocumentContent content = fhirService.getDocumentContent(documentReference);

        if (content == null) {
            return;
        }

        SolrInputDocument document = new SolrInputDocument();
        document.addField("RPT_ID", documentReference.getId());
        document.addField("MRN", fhirService.getMRN(patient));
        document.addField("RPT_DATE", documentReference.getCreated());
        document.addField("SOURCE", "source1");
        document.addField("RPT_TEXT", content.getContent());

        try {
            solrClient.add(document);
            solrClient.commit();
        } catch (Exception e) {
            log.error("Error indexing document", e);
        }
    }

}

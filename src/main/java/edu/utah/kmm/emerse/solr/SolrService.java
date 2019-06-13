package edu.utah.kmm.emerse.solr;

import edu.utah.kmm.emerse.fhir.FhirService;
import edu.utah.kmm.emerse.model.DocumentContent;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.impl.XMLResponseParser;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;
import org.hl7.fhir.dstu3.model.DocumentReference;
import org.hl7.fhir.dstu3.model.Patient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.util.List;

/**
 * Solr-related services.
 */
public class SolrService {

    private static final Log log = LogFactory.getLog(SolrService.class);

    private final HttpSolrClient solrClient;

    private final String username;

    private final String password;

    @Autowired
    private FhirService fhirService;

    public SolrService(String baseSolrUrl, String username, String password) {
        this.username = username;
        this.password = password;

        solrClient = new HttpSolrClient.Builder(baseSolrUrl)
                .withResponseParser(new XMLResponseParser())
                .allowCompression(true)
                .build();
    }

    public int batchIndex(Resource source) {
        try {
            return batchIndex(IOUtils.readLines(source.getInputStream(), "UTF-8"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public int batchIndex(List<String> mrns) {
        return mrns.size();
    }

    public void indexDocuments(Patient patient) {
        List<DocumentReference> documents = fhirService.getDocuments(patient.getId());
        String mrn = fhirService.getMRN(patient);

        for (DocumentReference document: documents) {
            indexDocument(mrn, document);
         }
    }

    public UpdateResponse indexDocument(String mrn, DocumentReference documentReference) {
        DocumentContent content = fhirService.getDocumentContent(documentReference);

        if (content == null) {
            log.warn("Document has no content: " + documentReference.getId());
            return null;
        }

        SolrInputDocument document = new SolrInputDocument();
        document.addField("RPT_ID", documentReference.getId());
        document.addField("MRN", mrn);
        document.addField("RPT_DATE", documentReference.getCreated());
        document.addField("SOURCE", "source1");
        document.addField("RPT_TEXT", content.getContent());

        try {
            UpdateRequest request = new UpdateRequest();
            request.add(document);
            request.setBasicAuthCredentials(username, password);
            return request.process(solrClient);
        } catch (Exception e) {
            log.error("Error indexing document: " + documentReference.getId(), e);
            return null;
        }
    }

}

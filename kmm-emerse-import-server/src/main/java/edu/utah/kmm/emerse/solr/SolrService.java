package edu.utah.kmm.emerse.solr;

import edu.utah.kmm.emerse.database.DatabaseService;
import edu.utah.kmm.emerse.fhir.FhirClient;
import edu.utah.kmm.emerse.model.DocumentContent;
import edu.utah.kmm.emerse.security.Credentials;
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

    private final Credentials credentials;

    @Autowired
    private FhirClient fhirClient;

    @Autowired
    private DatabaseService databaseService;

    public SolrService(String baseSolrUrl, Credentials credentials) {
        this.credentials = credentials;
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
        databaseService.createOrUpdatePatient(patient, true);
        List<DocumentReference> documents = fhirClient.getDocuments(patient.getId());
        String mrn = fhirClient.extractMRN(patient);

        for (DocumentReference document: documents) {
            indexDocument(mrn, document);
         }
    }

    public UpdateResponse indexDocument(String mrn, DocumentReference documentReference) {
        DocumentContent content = fhirClient.getDocumentContent(documentReference);

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
            request.setBasicAuthCredentials(credentials.getUsername(), credentials.getPassword());
            return request.process(solrClient);
        } catch (Exception e) {
            log.error("Error indexing document: " + documentReference.getId(), e);
            return null;
        }
    }

    public void index(IndexRequest request) {
        boolean isMRN = request.identifierType == IndexRequest.IdentifierType.MRN;

        for (String id: request.patientList) {
            Patient patient = isMRN ? fhirClient.getPatientByMrn(id) : fhirClient.getPatientById(id);
            indexDocuments(patient);
        }
    }
}

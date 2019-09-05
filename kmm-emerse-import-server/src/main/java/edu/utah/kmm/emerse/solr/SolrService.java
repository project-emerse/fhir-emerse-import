package edu.utah.kmm.emerse.solr;

import edu.utah.kmm.emerse.database.DatabaseService;
import edu.utah.kmm.emerse.fhir.FhirClient;
import edu.utah.kmm.emerse.model.DocumentContent;
import edu.utah.kmm.emerse.model.IdentifierType;
import edu.utah.kmm.emerse.security.Credentials;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.impl.XMLResponseParser;
import org.apache.solr.client.solrj.request.UpdateRequest;
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

    private static final String DOCUMENTS = "documents";

    private final HttpSolrClient solrClient;

    private final Credentials solrCredentials;

    @Autowired
    private FhirClient fhirClient;

    @Autowired
    private DatabaseService databaseService;

    public SolrService(String solrServerRoot, Credentials solrCredentials) {
        this.solrCredentials = solrCredentials;
        solrClient = new HttpSolrClient.Builder(solrServerRoot)
                .withResponseParser(new XMLResponseParser())
                .allowCompression(true)
                .build();
    }

    /**
     * Index documents for a resource containing a list of patient id's.
     *
     * @param source The resource containing a list of patient id's.
     * @param type The type of id.
     * @return The indexing result.
     */
    public IndexResult batchIndex(Resource source, IdentifierType type) {
        try {
            return batchIndex(IOUtils.readLines(source.getInputStream(), "UTF-8"), type);
        } catch (IOException e) {
            return new IndexResult();
        }
    }

    /**
     * Index documents for a list of patient id's.
     *
     * @param ids A list of patient id's.
     * @param type The type of id.
     * @return The indexing result.
     */
    public IndexResult batchIndex(List<String> ids, IdentifierType type) {
        IndexResult result = new IndexResult();

        for (String id: ids) {
            result.combine(indexDocuments(id, type));
        }

        return result;
    }

    /**
     * Index all documents for a given patient.
     *
     * @param patient The patient resource.
     * @return The indexing result.
     */
    public IndexResult indexDocuments(Patient patient) {
        IndexResult result = new IndexResult();
        databaseService.createOrUpdatePatient(patient, true);
        List<DocumentReference> documents = fhirClient.getDocumentsById(patient.getId());
        String mrn = fhirClient.extractMRN(patient);

        for (DocumentReference document: documents) {
            result.combine(indexDocument(mrn, document));
         }

        commit();
        return result;
    }

    /**
     * Index all documents for a given patient.
     *
     * @param id The patient's id.
     * @param type The id type.
     * @return The indexing result.
     */
    public IndexResult indexDocuments(String id, IdentifierType type) {
        Patient patient = fhirClient.getPatient(id, type);
        return patient == null ? new IndexResult() : indexDocuments(patient);
    }

    /**
     * Index a single document.
     *
     * @param mrn MRN of the subject of the document.
     * @param documentReference The document to index.
     * @return The indexing result.
     */
    public IndexResult indexDocument(String mrn, DocumentReference documentReference) {
        IndexResult result = new IndexResult();
        DocumentContent content = fhirClient.getDocumentContent(documentReference);

        if (content == null || content.getContent() == null) {
            log.warn("Document has no content: " + documentReference.getId());
            return result.success(false);
        }

        SolrInputDocument document = new SolrInputDocument();
        String id = documentReference.getIdElement().getIdPart();
        document.addField("ID", id);
        document.addField("RPT_ID", id);
        document.addField("MRN", mrn);
        document.addField("RPT_DATE", documentReference.getCreated());
        document.addField("SOURCE", "source1");
        document.addField("RPT_TEXT", content.getContent());

        try {
            UpdateRequest request = new UpdateRequest();
            request.add(document);
            request.setBasicAuthCredentials(solrCredentials.getUsername(), solrCredentials.getPassword());
            request.process(solrClient, DOCUMENTS);
            return result.success(true);
        } catch (Exception e) {
            log.error("Error indexing document: " + documentReference.getId(), e);
            return result.success(false);
        }
    }

    /**
     * Service an index request.
     *
     * @param request The index request.
     * @return The indexing result.
     */
    public IndexResult index(IndexRequest request) {
        IndexResult result = new IndexResult();

        for (String id: request.patientList) {
            Patient patient = fhirClient.getPatient(id, request.identifierType);
            result.combine(indexDocuments(patient));
        }

        return result;
    }

    public void commit() {
        try {
            solrClient.commit(DOCUMENTS);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}

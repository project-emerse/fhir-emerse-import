package edu.utah.kmm.emerse.solr;

import edu.utah.kmm.emerse.database.DatabaseService;
import edu.utah.kmm.emerse.dto.*;
import edu.utah.kmm.emerse.fhir.FhirClient;
import edu.utah.kmm.emerse.model.IdentifierType;
import edu.utah.kmm.emerse.security.Credentials;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.impl.XMLResponseParser;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.common.SolrInputDocument;
import org.hl7.fhir.dstu3.model.DocumentReference;
import org.hl7.fhir.dstu3.model.Patient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Solr-related services.
 */
public class SolrService {

    private static final Log log = LogFactory.getLog(SolrService.class);

    private static final String COLLECTION_DOCUMENTS = "documents";

    private static final String COLLECTION_PATIENT = "patient";

    private static final String COLLECTION_SLAVE = "patient-slave";

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
     * Service an index request.
     *
     * @param request The index request.
     * @return The indexing result.
     */
    public IndexResult batchIndexImmediate(IndexRequestDTO request) {
        IndexResult result = new IndexResult();

        for (String id: request) {
            id = id.trim();

            if (!id.isEmpty()) {
                result.combine(indexDocuments(id, request.getIdentifierType()));
            }

            if (result.getTotal() % 100 == 0) {
                databaseService.createOrUpdateIndexRequest(request);
            }
        }

        databaseService.createOrUpdateIndexRequest(request);
        return result;
    }

    public void batchIndexQueued(IndexRequestDTO request) {
        databaseService.createOrUpdateIndexRequest(request);
    }

    /**
     * Index all documents for a given patient.
     *
     * @param patient The patient resource.
     * @return The indexing result.
     */
    public IndexResult indexDocuments(Patient patient) {
        IndexResult result = new IndexResult();
        databaseService.createOrUpdatePatient(patient, false);
        List<DocumentReference> documents = fhirClient.getDocumentsForPatient(patient.getId());
        String mrn = fhirClient.extractMRN(patient);

        for (DocumentReference document: documents) {
            result.combine(indexDocument(mrn, document));
         }

        commit();
        return result;
    }

    /**
     * Index all document(s) for a given id.
     *
     * @param id The id.
     * @param type The id type.
     * @return The indexing result.
     */
    public IndexResult indexDocuments(String id, IdentifierType type) {
        if (type == IdentifierType.DOCID) {
            return indexDocument(id);
        } else {
            Patient patient = fhirClient.getPatient(id, type);
            return patient == null ? new IndexResult() : indexDocuments(patient);
        }
    }

    /**
     * Index a single document.
     *
     * @param docid The document's FHIR id.
     * @return The indexing result.
     */
    public IndexResult indexDocument(String docid) {
        DocumentReference document = fhirClient.getDocumentById(docid);
        Assert.notNull(document, "Document could not be located.");
        String mrn = fhirClient.getPatientMrn(document);
        Assert.notNull(mrn, "Cannot determine subject of document.");
        return indexDocument(mrn, document);
    }

    /**
     * Index a single document.
     *
     * @param mrn MRN of the subject of the document.
     * @param document The document to index.
     * @return The indexing result.
     */
    public IndexResult indexDocument(String mrn, DocumentReference document) {
        IndexResult result = new IndexResult();
        ContentDTO content = fhirClient.getDocumentContent(document);

        if (content.isEmpty()) {
            log.warn("Document has no content: " + document.getId());
            return result.success(false);
        }

        Map<String, Object> map = new HashMap<>();
        map.putAll(content.getMap());
        map.put("MRN", mrn);
        return result.success(indexDTO(new DocumentDTO(document, map), COLLECTION_DOCUMENTS));
    }

    /**
     * Service an index request.
     *
     * @param request The index request.
     * @return The indexing result.
     */
    public IndexResult index(IndexRequestDTO request) {
        IndexResult result = new IndexResult();

        for (String id: request) {
            result.combine(indexDocuments(id, request.getIdentifierType()));
        }

        return result;
    }

    public void commit() {
        try {
            solrClient.commit(COLLECTION_DOCUMENTS);
            solrClient.commit(COLLECTION_PATIENT);
            solrClient.commit(COLLECTION_SLAVE);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private boolean indexDTO(BaseDTO dto, String collection) {
        try {
            UpdateRequest request = new UpdateRequest();
            request.add(newSolrDocument(dto.getMap()));
            request.process(solrClient, collection);
            return true;
        } catch (SolrServerException | IOException e) {
            log.error("Error indexing entity for collection " + collection, e);
            return false;
        }
    }

    private SolrInputDocument newSolrDocument(Map<String, Object> fields) {
        SolrInputDocument solrDoc = new SolrInputDocument();

        for (Map.Entry<String, Object> entry: fields.entrySet()) {
            solrDoc.addField(entry.getKey(), entry.getValue());
        }

        return solrDoc;
    }

    public void indexPatient(PatientDTO patientDTO) {
        indexDTO(patientDTO, COLLECTION_PATIENT);
        indexDTO(patientDTO, COLLECTION_SLAVE);
    }

    public void indexDocument(DocumentDTO documentDTO) {
        indexDTO(documentDTO, COLLECTION_DOCUMENTS);
    }
}

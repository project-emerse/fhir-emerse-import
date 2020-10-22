package edu.utah.kmm.emerse.solr;

import edu.utah.kmm.emerse.database.BaseDTO;
import edu.utah.kmm.emerse.database.DatabaseService;
import edu.utah.kmm.emerse.document.ContentDTO;
import edu.utah.kmm.emerse.document.DocumentDTO;
import edu.utah.kmm.emerse.document.DocumentService;
import edu.utah.kmm.emerse.fhir.IdentifierType;
import edu.utah.kmm.emerse.patient.PatientDTO;
import edu.utah.kmm.emerse.patient.PatientService;
import edu.utah.kmm.emerse.security.Credentials;
import edu.utah.kmm.emerse.solr.IndexRequestDTO.IndexRequestStatus;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.impl.XMLResponseParser;
import org.apache.solr.client.solrj.request.GenericSolrRequest;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.MapSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.codehaus.janino.util.Producer;
import org.hl7.fhir.dstu3.model.DocumentReference;
import org.hl7.fhir.dstu3.model.Patient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

import java.io.IOException;
import java.util.*;

/**
 * Solr-related services.
 */
public class SolrService {

    private static final Log log = LogFactory.getLog(SolrService.class);

    private static final String COLLECTION_DOCUMENTS = "documents";

    private static final String COLLECTION_PATIENT = "patient";

    private static final String COLLECTION_SLAVE = "patient-slave";

    private final HttpSolrClient solrClient;

    private final String solrUsername;

    private final String solrPassword;

    private final Map<String, Producer<IndexRequestDTO>> processing = new HashMap<>();

    @Autowired
    private DocumentService documentService;

    @Autowired
    private PatientService patientService;

    @Autowired
    private DatabaseService databaseService;

    @Autowired
    private IndexRequestQueue indexRequestQueue;

    public SolrService(
            String solrServerRoot,
            Credentials solrServiceCredentials) {
        solrUsername = solrServiceCredentials.getUsername();
        solrPassword = solrServiceCredentials.getPassword();
        solrClient = new HttpSolrClient.Builder(solrServerRoot)
                .withResponseParser(new XMLResponseParser())
                .allowCompression(true)
                .build();
    }

    public String getSolrVersion() {
        try {
            SolrParams params = new MapSolrParams(Collections.singletonMap("wt", "xml"));
            GenericSolrRequest request = new GenericSolrRequest(SolrRequest.METHOD.GET, "/admin/info/system", params);
            NamedList<?> result = solrClient.httpUriRequest(request).future.get();
            NamedList<?> versions = (NamedList<?>) result.get("lucene");
            String version = Objects.toString(versions.get("solr-impl-version"));
            return version == null ? null : ("Solr Release " + version);
        } catch (Exception e) {
            return null;
        }
    }

    public IndexRequestDTO createIndexRequest(Resource resource) {
        return addCloseCallbacks(new IndexRequestDTO(resource));
    }

    public IndexRequestDTO createIndexRequest(String requestId) {
        return addCloseCallbacks(databaseService.fetchRequest(requestId));
    }

    private IndexRequestDTO addCloseCallbacks(IndexRequestDTO request) {
        request.registerCloseCallback(req -> databaseService.updateIndexRequest(req));
        request.registerCloseCallback(req -> lockProcessing(() -> processing.remove(req.getId())));
        return request;
    }

    /**
     * Service an index request.
     *
     * @param resource The resource containing the serialized index request.
     * @return The indexing result.
     */
    public IndexResult batchIndexImmediate(Resource resource) {
        return processRequest(createIndexRequest(resource));
    }

    private <T> T lockProcessing(Producer<T> operation) {
        synchronized (processing) {
            return operation.produce();
        }
    }

    public void batchIndexQueued(Resource resource) {
        createIndexRequest(resource).close();
        indexRequestQueue.refreshNow();
    }

    public IndexRequestDTO indexRequestAction(IndexRequestAction action) {
        IndexRequestDTO request = lockProcessing(() -> {
            Producer<IndexRequestDTO> producer = processing.get(action.id);
            IndexRequestDTO req = producer == null ? null : producer.produce();

            if (req != null) {
                indexRequestAction(req, action);
            }

            return req;
        });

        if (request == null) {
            request = createIndexRequest(action.id);
            indexRequestAction(request, action);
            request.close();
        }

        return request;
    }

    private void indexRequestAction(
            IndexRequestDTO request,
            IndexRequestAction action) {
        switch (action.action) {
            case ABORT:
                request.abort();
                break;
            case RESUME:
                request.resume();
                indexRequestQueue.refreshNow();
                break;
            case SUSPEND:
                request.suspend();
                break;
            case DELETE:
                request.delete();
                break;
            case RESTART:
                request.restart();
                indexRequestQueue.refreshNow();
                break;
        }
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
        List<DocumentReference> documents = documentService.getDocumentsForPatient(patient.getId(), IdentifierType.PATID);
        String mrn = patientService.extractMRN(patient);

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
            Patient patient = patientService.getPatient(id, type);
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
        DocumentReference document = documentService.getDocumentById(docid);
        Assert.notNull(document, "Document could not be located");
        String mrn = documentService.extractMRN(document);
        Assert.notNull(mrn, "Cannot determine subject of document");
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
        ContentDTO content = documentService.getDocumentContent(document);

        if (content.isEmpty()) {
            log.warn("Document has no content: " + document.getId());
            return result.success(false);
        }

        Map<String, Object> map = new HashMap<>(content.getMap());
        map.put("MRN", mrn);
        return result.success(indexDTO(new DocumentDTO(document, map), COLLECTION_DOCUMENTS));
    }

    public void processRequest(String requestId) {
        Producer<IndexRequestDTO> producer = lockProcessing(() -> {
            if (processing.containsKey(requestId)) {
                return null;
            }

            Producer<IndexRequestDTO> prod = new Producer<IndexRequestDTO>() {
                private IndexRequestDTO request;

                @Override
                public IndexRequestDTO produce() {
                    return request == null ? request = createIndexRequest(requestId) : request;
                }
            };

            processing.put(requestId, prod);
            return prod;
        });

        if (producer != null) {
            processRequest(producer.produce());
        }
    }

    /**
     * Service an index request.
     *
     * @param indexRequestDTO The index request.
     */
    private IndexResult processRequest(IndexRequestDTO indexRequestDTO) {
        IndexResult result = new IndexResult();

        try (IndexRequestDTO request = indexRequestDTO) {
            if (request.getStatus() != IndexRequestStatus.QUEUED) {
                return result;
            }

            request.start();
            IdentifierType identifierType = request.getIdentifierType();

            for (String id : request.getIdentifiers(true)) {
                try {
                    if (request.getStatus() != IndexRequestStatus.RUNNING) {
                        break;
                    }

                    if (result.getTotal() % 50 == 0) {
                        databaseService.updateIndexRequest(request);
                    }

                    result.combine(indexDocuments(id, identifierType));
                    request.processed();

                } catch (Exception e) {
                    request.error(e.getMessage());
                }
            }

            if (request.getStatus() == IndexRequestStatus.RUNNING) {
                request.completed();
            }
        }

        return result;
    }

    public void commit() {
        try {
            solrClient.commit(COLLECTION_DOCUMENTS);
            solrClient.commit(COLLECTION_PATIENT);
            solrClient.commit(COLLECTION_SLAVE);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private boolean indexDTO(BaseDTO dto, String collection) {
        try {
            UpdateRequest request = new UpdateRequest();
            request.setBasicAuthCredentials(solrUsername, solrPassword);
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

}

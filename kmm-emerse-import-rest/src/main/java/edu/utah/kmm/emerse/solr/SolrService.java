package edu.utah.kmm.emerse.solr;

import edu.utah.kmm.emerse.database.DatabaseService;
import edu.utah.kmm.emerse.document.ContentDTO;
import edu.utah.kmm.emerse.document.DocumentDTO;
import edu.utah.kmm.emerse.document.DocumentService;
import edu.utah.kmm.emerse.fhir.IdentifierType;
import edu.utah.kmm.emerse.patient.PatientDTO;
import edu.utah.kmm.emerse.patient.PatientService;
import edu.utah.kmm.emerse.security.Credentials;
import edu.utah.kmm.emerse.solr.IndexRequestDTO.IndexRequestStatus;
import edu.utah.kmm.emerse.util.MiscUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.request.GenericSolrRequest;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.MapSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.hl7.fhir.dstu3.model.DocumentReference;
import org.hl7.fhir.dstu3.model.Patient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

import javax.annotation.PreDestroy;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Solr-related services.
 */
public class SolrService {

    private static final Log log = LogFactory.getLog(SolrService.class);

    private static final String COLLECTION_DOCUMENTS = "documents";

    private static final String COLLECTION_PATIENT = "patient";

    private static final String COLLECTION_SLAVE = "patient-slave";

    private static final String RPT_DATE = "RPT_DATE";

    private final HttpSolrClient solrClient;

    @Autowired
    private DocumentService documentService;

    @Autowired
    private PatientService patientService;

    @Autowired
    private DatabaseService databaseService;

    @Autowired
    private IndexRequestFactory indexRequestFactory;

    @Autowired
    private IndexRequestQueue indexRequestQueue;

    public SolrService(
            String solrServerRoot,
            Credentials credentials) {
        String auth = String.format("%s:%s", credentials.getUsername(), credentials.getPassword());
        String header = "Basic " + Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.ISO_8859_1));
        HttpClient client = HttpClientBuilder.create()
                .addInterceptorFirst((HttpRequestInterceptor) (httpRequest, httpContext) ->
                        httpRequest.setHeader(HttpHeaders.AUTHORIZATION, header))
                .build();
        solrClient = new HttpSolrClient.Builder(solrServerRoot)
                .withHttpClient(client)
                .allowCompression(true)
                .build();
    }

    public String getSolrVersion() {
        try {
            SolrParams solrParams = new MapSolrParams(Collections.emptyMap());
            GenericSolrRequest request = new GenericSolrRequest(SolrRequest.METHOD.GET, "/admin/info/system", solrParams);
            String version = extractValue("lucene/solr-impl-version", solrClient.request(request), String.class);
            return version == null ? null : ("Solr Release " + version);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Extracts a value from a named list following the specified path.
     *
     * @param path A "/"-delimited path.
     * @param list The named list to search.
     * @param type The expected return type.
     * @param <T>  The expected return type.
     * @return The value at the specified path (possibly null).
     */
    private <T> T extractValue(
            String path,
            NamedList<?> list,
            Class<T> type) {
        Object value = list.findRecursive(path.split("/"));
        return type.isInstance(value) ? (T) value : null;
    }

    /**
     * Service an index request.
     *
     * @param resource The resource containing the serialized index request.
     * @return The indexing result.
     */
    public IndexResult batchIndexImmediate(Resource resource) {
        return processRequest(indexRequestFactory.create(resource).get());
    }

    /**
     * Creates an index request from a resource, then queues it.
     *
     * @param resource Resource containing the index request.
     */
    public void batchIndexQueued(Resource resource) {
        indexRequestFactory.create(resource).get().close();
        indexRequestQueue.refreshNow();
    }

    /**
     * Performs an action on an index request.
     *
     * @param action The action to perform.
     * @return The index request upon which the action was performed.
     */
    public IndexRequestDTO indexRequestAction(IndexRequestAction action) {
        IndexRequestWrapper wrapper = indexRequestFactory.create(action.id, true);
        boolean hydrated = wrapper.isHydrated();
        IndexRequestDTO request = wrapper.get();

        if (request.performAction(action.action) && !hydrated) {
            request.close();
            indexRequestQueue.refreshNow();
        }

        return request;
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
        map.put("SOURCE", "source4");

        try {
            indexDTO(new DocumentDTO(document, map), COLLECTION_DOCUMENTS);
            return result.success(true);
        } catch (Exception e) {
            return result.success(false);
        }
    }

    /**
     * Processes the index request associated with the given ID.
     *
     * @param requestId The ID of the index request.
     */
    public void processRequest(String requestId) {
        IndexRequestWrapper wrapper = indexRequestFactory.create(requestId, true);

        if (!wrapper.isHydrated()) {
            processRequest(wrapper.get());
        }
    }

    /**
     * Service an index request.
     *
     * @param indexRequestDTO The index request.
     */
    public IndexResult processRequest(IndexRequestDTO indexRequestDTO) {
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

                    if (result.getTotal() % 20 == 0) {
                        databaseService.updateIndexRequest(request);
                        commit();
                    }

                    result.combine(indexDocuments(id, identifierType));
                    request.processed();

                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                    request.error(e.getMessage());
                }
            }

            if (request.getStatus() == IndexRequestStatus.RUNNING) {
                request.completed();
            }
        }

        commit();
        return result;
    }

    /**
     * Commit all outstanding index transactions.
     */
    @PreDestroy
    public void commit() {
        try {
            solrClient.commit(COLLECTION_DOCUMENTS);
            solrClient.commit(COLLECTION_PATIENT);
            solrClient.commit(COLLECTION_SLAVE);
            updateIndexSummary();
        } catch (Exception e) {
            MiscUtil.rethrow(e);
        }
    }

    /**
     * Index the object (patient or document) represented by the DTO.
     *
     * @param dto The DTO (patient or document).
     * @param collection The document collection for the index.
     */
    private void indexDTO(
            BaseSolrDTO dto,
            String collection) {
        try {
            UpdateRequest request = new UpdateRequest();
            request.add(newSolrDocument(dto.getSolrMap()));
            solrClient.request(request, collection);
        } catch (Exception e) {
            log.error("Error indexing entity for collection " + collection, e);
            MiscUtil.rethrow(e);
        }
    }

    /**
     * Creates a SOLR input document for indexing.
     *
     * @param fields The fields' names and values.
     * @return The newly created SOLR input document.
     */
    private SolrInputDocument newSolrDocument(Map<String, Object> fields) {
        SolrInputDocument solrDoc = new SolrInputDocument();

        for (Map.Entry<String, Object> entry : fields.entrySet()) {
            solrDoc.addField(entry.getKey(), entry.getValue());
        }

        return solrDoc;
    }

    /**
     * Indexes a patient.
     *
     * @param patientDTO The patient to index.
     */
    public void indexPatient(PatientDTO patientDTO) {
        indexDTO(patientDTO, COLLECTION_PATIENT);
        indexDTO(patientDTO, COLLECTION_SLAVE);
        commit();
    }

    /**
     * Deletes all indexes within a collection.
     *
     * @param collection The collection containing the indexes to delete.
     */
    private void deleteCollection(String collection) {
        try {
            UpdateRequest request = new UpdateRequest();
            request.deleteByQuery("*:*");
            request.commit(solrClient, collection);
        } catch (Exception e) {
            MiscUtil.rethrow(e);
        }
    }

    /**
     * Delete indexes for both patient collections.
     */
    public void deletePatientCollections() {
        deleteCollection(COLLECTION_PATIENT);
        deleteCollection(COLLECTION_SLAVE);
    }

    /**
     * Delete indexes for the document collection.
     */
    public void deleteDocumentCollection() {
        deleteCollection(COLLECTION_DOCUMENTS);
    }

    /**
     * Update the SOLR index summary.
     */
    public void updateIndexSummary() {
        try {
            SolrQuery query = new SolrQuery();
            query.setRows(0);
            query.setQuery("*:*");
            SolrDocumentList results = solrClient.query(COLLECTION_PATIENT, query).getResults();
            long patientCount = results.getNumFound();
            Date start = getDateBound(true);
            Date end = getDateBound(false);
            databaseService.updateSolrIndexSummary(start, end, patientCount);
        } catch (Exception e) {
            MiscUtil.rethrow(e);
        }
    }

    /**
     * Returns the earliest or latest document date.
     *
     * @param earliest If true, return earliest, otherwise latest.
     * @return The requested date (possibly null).
     */
    private Date getDateBound(boolean earliest) {
        try {
            SolrQuery query = new SolrQuery();
            query.setQuery("*:*");
            query.setFields(RPT_DATE);
            query.setSort(RPT_DATE, earliest ? SolrQuery.ORDER.asc : SolrQuery.ORDER.desc);
            query.setRows(1);
            SolrDocumentList results = solrClient.query(COLLECTION_DOCUMENTS, query).getResults();
            return results.isEmpty() ? null : (Date) results.get(0).getFieldValue(RPT_DATE);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    /**
     * Reset the SOLR index summary.
     */
    public void resetIndexSummary() {
        databaseService.updateSolrIndexSummary(null, null, 0);
    }

    /**
     * Returns status information for patient and documents indexes.
     */
    public Map<String, Object>[] getIndexStatus() {
        try {
            Map<String, String> params = new HashMap<>();
            params.put("action", "STATUS");
            SolrParams solrParams = new MapSolrParams(params);
            GenericSolrRequest request = new GenericSolrRequest(SolrRequest.METHOD.GET, "/admin/cores", solrParams);
            NamedList<?> response = solrClient.request(request);
            return new Map[]{
                    getIndexStatus(COLLECTION_PATIENT, response),
                    getIndexStatus(COLLECTION_DOCUMENTS, response)
            };
        } catch (Exception e) {
            return MiscUtil.rethrow(e);
        }
    }

    /**
     * Extract status information for the specified collection.
     *
     * @param collection The collection whose status is to be retrieved.
     * @param response The response containing the status information.
     * @return The status of the specified collection.
     */
    private Map<String, Object> getIndexStatus(
            String collection,
            NamedList<?> response) {
        NamedList<?> status = extractValue("status/" + collection + "/index", response, NamedList.class);
        Map<String, Object> map = status == null ? new HashMap<>() : status.asMap(2);
        map.put("collection", collection);
        return map;
    }

}

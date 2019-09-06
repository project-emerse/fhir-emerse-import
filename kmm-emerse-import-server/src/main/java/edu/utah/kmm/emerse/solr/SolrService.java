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
import org.springframework.util.Assert;

import java.io.IOException;
import java.util.List;

import static edu.utah.kmm.emerse.util.MiscUtil.toIdentifierType;

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
     * @return The indexing result.
     */
    public IndexResult batchIndex(Resource source) {
        try {
            return batchIndex(IOUtils.readLines(source.getInputStream(), "UTF-8"));
        } catch (IOException e) {
            return new IndexResult();
        }
    }

    /**
     * Index documents for a list of patient id's.
     *
     * @param ids A list of patient id's.
     * @return The indexing result.
     */
    public IndexResult batchIndex(List<String> ids) {
        IndexResult result = new IndexResult();
        IdentifierType type = toIdentifierType(ids.isEmpty() ? "" : ids.remove(0).trim());
        Assert.notNull(type, "An valid identifier type was not found.");

        for (String id: ids) {
            id = id.trim();

            if (!id.isEmpty()) {
                result.combine(indexDocuments(id, type));
            }
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
        DocumentContent content = fhirClient.getDocumentContent(document);

        if (content == null || content.getContent() == null) {
            log.warn("Document has no content: " + document.getId());
            return result.success(false);
        }

        SolrInputDocument solrDoc = new SolrInputDocument();
        String id = document.getIdElement().getIdPart();
        solrDoc.addField("ID", id);
        solrDoc.addField("RPT_ID", id);
        solrDoc.addField("MRN", mrn);
        solrDoc.addField("RPT_DATE", document.getCreated());
        solrDoc.addField("SOURCE", "source1");
        solrDoc.addField("RPT_TEXT", content.getContent());

        try {
            UpdateRequest request = new UpdateRequest();
            request.add(solrDoc);
            request.setBasicAuthCredentials(solrCredentials.getUsername(), solrCredentials.getPassword());
            request.process(solrClient, DOCUMENTS);
            return result.success(true);
        } catch (Exception e) {
            log.error("Error indexing document: " + document.getId(), e);
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

        for (String id: request.identifiers) {
            result.combine(indexDocuments(id, request.identifierType));
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

package edu.utah.kmm.emerse.servlet;

import edu.utah.kmm.emerse.config.ClientConfigService;
import edu.utah.kmm.emerse.database.DatabaseService;
import edu.utah.kmm.emerse.document.ContentDTO;
import edu.utah.kmm.emerse.document.DocumentService;
import edu.utah.kmm.emerse.fhir.FhirService;
import edu.utah.kmm.emerse.fhir.IdentifierType;
import edu.utah.kmm.emerse.patient.PatientService;
import edu.utah.kmm.emerse.solr.IndexRequestDTO;
import edu.utah.kmm.emerse.solr.IndexResult;
import edu.utah.kmm.emerse.solr.SolrService;
import edu.utah.kmm.emerse.util.MiscUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hl7.fhir.dstu3.model.DocumentReference;
import org.hl7.fhir.dstu3.model.Patient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.*;

/**
 * Controller for all REST services.
 *
 */
@Controller
@RequestMapping("/api")
@CrossOrigin
public class RestController {

    private static final Log log = LogFactory.getLog(RestController.class);

    @Autowired
    private FhirService fhirService;

    @Autowired
    private PatientService patientService;

    @Autowired
    private DocumentService documentService;

    @Autowired
    private SolrService solrService;

    @Autowired
    private DatabaseService databaseService;

    @Autowired
    private ClientConfigService clientConfigService;

    @GetMapping("/login")
    @ResponseBody
    public boolean login(Principal user) {
        return user != null;
    }

    @GetMapping("/config")
    @ResponseBody
    public Map<String, String> getConfiguration() {
        return clientConfigService.getConfig();
    }

    @RequestMapping(
            path = "/ping",
            method = RequestMethod.GET,
            produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String ping() {
        return "<h1>EMERSE-IT server is running.</h1>";
    }

    /**
     * Fetch patient from FHIR service.
     *
     * @param mrn The patient's MRN.
     * @param patid The patient's FHIR id.
     * @return Serialized form of the Patient resource.
     */
    @GetMapping("/patient")
    @ResponseBody
    public String getPatient(
            @RequestParam(required = false) String mrn,
            @RequestParam(required = false) String patid) {
        IdentifierType type = validateIdentifiers(mrn, patid);
        return fhirService.serialize(patientService.getPatient(mrn != null ? mrn : patid, type));
    }

    /**
     * Create/update entry in EMERSE patient table.
     *
     * @param payload Serialized form of the Patient resource.
     * @return Status of the operation.
     */
    @PostMapping("/patient")
    public ResponseEntity updatePatient(
            @RequestBody String payload) {
        Patient patient = fhirService.deserialize(payload, Patient.class);
        databaseService.createOrUpdatePatient(patient, true);
        return new ResponseEntity(HttpStatus.OK);
    }

    /**
     * Fetch a patient's documents.
     *
     * @param patid The patient's FHIR id.
     * @return A list of documents.
     */
    @GetMapping("/documents")
    @ResponseBody
    public List<?> getDocuments(
            @RequestParam String patid) {
        List<Map<String, Object>> docs = new ArrayList<>();

        for (DocumentReference document: documentService.getDocumentsForPatient(patid)) {
            ContentDTO documentContent = documentService.getDocumentContent(document);

            if (documentContent != null) {
                Map<String, Object> map = new HashMap<>();
                Date date = document.hasCreated() ? document.getCreated() : null;
                map.put("id", document.getIdElement().getIdPart());
                map.put("title", document.getType().getText());
                map.put("date", date == null ? null :  date.getTime());
                map.put("dateStr", date == null ? null : MiscUtil.dateTimeParser.format(date));
                map.put("body", documentContent.getContent());
                map.put("isHtml", "text/html".equals(documentContent.getContentType()));
                docs.add(map);
            }
        }

        return docs;
    }

    /**
     * Index a patient's documents.
     *
     * @param mrn The patient's MRN.
     * @param patid The patient's FHIR id.
     * @return Result of the indexing request.
     */
    @GetMapping("/index")
    @ResponseBody
    public IndexResult indexDocumentsByPatient(
            @RequestParam(required = false) String mrn,
            @RequestParam(required = false) String patid) {
        return solrService.indexDocuments(mrn != null ? mrn : patid, validateIdentifiers(mrn, patid));
    }

    /**
     * Run batch index in foreground.
     *
     * @param file File containing list of id's.
     * @return The indexing result.
     */
    @PostMapping("/batch-fg")
    @ResponseBody
    public IndexResult indexBatchImmediate(
            @RequestParam MultipartFile file) {
        return solrService.batchIndexImmediate(new IndexRequestDTO(file.getResource()) );
    }

    /**
     * Run batch index in background
     *
     * @param file File containing list of id's.
     * @return The indexing result.
     */
    @PostMapping("/batch-bg")
    @ResponseBody
    public ResponseEntity indexBatchQueued(
            @RequestParam MultipartFile file) {
        solrService.batchIndexQueued(new IndexRequestDTO(file.getResource()));
        return new ResponseEntity(HttpStatus.OK);
    }

    /**
     * Fetches entries from the queue table.
     *
     * @return Entries from the queue table.
     */
    @GetMapping("/queue")
    @ResponseBody
    public List<?> fetchQueueEntries() {
        return databaseService.fetchQueueEntries();
    }

    /**
     * Validates that a single identifier is present.
     *
     * @param mrn The MRN.
     * @param patid The FHIR id.
     * @return The identifier type.
     * @throws IllegalArgumentException if both identifiers are null or both are non-null.
     */
    private IdentifierType validateIdentifiers(String mrn, String patid) {
        Assert.isTrue(mrn != null ^ patid != null, () -> mrn == null
                ? "You must specify an identifier (mrn or patid)"
                : "You may specify one and only one identifier (mrn or patid)");
        return mrn != null ? IdentifierType.MRN : IdentifierType.PATID;
    }

}

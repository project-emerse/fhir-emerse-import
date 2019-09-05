package edu.utah.kmm.emerse.servlet;

import edu.utah.kmm.emerse.config.ClientConfigService;
import edu.utah.kmm.emerse.database.DatabaseService;
import edu.utah.kmm.emerse.fhir.FhirClient;
import edu.utah.kmm.emerse.model.DocumentContent;
import edu.utah.kmm.emerse.solr.SolrService;
import edu.utah.kmm.emerse.util.MiscUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.DocumentReference;
import org.hl7.fhir.dstu3.model.Patient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
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
public class RestController {

    private static final Log log = LogFactory.getLog(RestController.class);

    @Autowired
    private FhirClient fhirClient;

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

    /**
     * Fetch patient from FHIR service.
     *
     * @param mrn
     * @return
     */
    @GetMapping("/patient/{mrn}")
    @ResponseBody
    public String getPatientByMrn(@PathVariable("mrn") String mrn) {
        return fhirClient.serialize(fhirClient.getPatientByMrn(mrn));
    }

    /**
     * Create/update entry in EMERSE patient table.
     *
     * @param payload Serialized form of patient.
     * @return
     */
    @PostMapping("/patient")
    public ResponseEntity updatePatient(@RequestBody String payload) {
        Patient patient = fhirClient.deserialize(payload, Patient.class);
        databaseService.createOrUpdatePatient(patient, true);
        return new ResponseEntity(HttpStatus.OK);
    }

    /**
     * Fetch a patient's documents.
     *
     * @param patientId
     * @return
     */
    @GetMapping("/documents/{patientId:.+}")
    @ResponseBody
    public List<?> getDocuments(@PathVariable("patientId") String patientId) {
        List<Map<String, Object>> docs = new ArrayList<>();
        Bundle bundle = fhirClient.getDocumentBundle(patientId);

        for (Bundle.BundleEntryComponent entry: bundle.getEntry()) {
            DocumentReference documentReference = (DocumentReference) entry.getResource();
            DocumentContent documentContent = fhirClient.getDocumentContent(documentReference);

            if (documentContent != null) {
                Map<String, Object> map = new HashMap<>();
                Date date = documentReference.hasCreated() ? documentReference.getCreated() : null;
                map.put("title", documentReference.getType().getText());
                map.put("date", date == null ? null :  date.getTime());
                map.put("dateStr", date == null ? null : MiscUtil.dateTimeParser.format(date));
                map.put("body", documentContent.getContent());
                map.put("isHtml", "text/html".equals(documentContent.getContentType()));
                docs.add(map);
            }
        }

        return docs;
    }

    @GetMapping("/index/{mrn}")
    @ResponseBody
    public int indexPatient(@PathVariable("mrn") String mrn) {
        return solrService.batchIndex(Collections.singletonList(mrn));
    }

    /**
     * Batch index.
     *
     * @param file
     * @return Count of patients indexed.
     */
    @PostMapping("/batch")
    @ResponseBody
    public int indexBatch(@RequestParam("file") MultipartFile file) {
        return solrService.batchIndex(file.getResource());
    }

}

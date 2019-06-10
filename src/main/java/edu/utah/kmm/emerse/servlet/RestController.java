package edu.utah.kmm.emerse.servlet;

import edu.utah.kmm.emerse.database.DatabaseService;
import edu.utah.kmm.emerse.fhir.FhirService;
import edu.utah.kmm.emerse.model.DocumentContent;
import edu.utah.kmm.emerse.solr.SolrService;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for all REST services.
 *
 */
@Controller
public class RestController {

    private static final Log log = LogFactory.getLog(RestController.class);

    private class Credentials {
        String username;
        String password;
    }

    @Autowired
    private FhirService fhirService;

    @Autowired
    private SolrService solrService;

    @Autowired
    private DatabaseService databaseService;

    @GetMapping(path = "/config")
    public Map<String, String> getConfiguration() {
        Map<String, String> config = new HashMap<>();
        config.put("fhir.mrn.system", fhirService.getMrnSystem());
        return config;
    }

    /**
     * Authenticate user.
     *
     * @param credentials The user's credentials.
     * @return OK if success; UNAUTHORIZED otherwise.
     */
    @PostMapping(path = "/authenticate")
    public ResponseEntity authenticate(@RequestBody Credentials credentials) {
        if (databaseService.authenticate(credentials.username, credentials.password)) {
            return new ResponseEntity(HttpStatus.OK);
        }

        return new ResponseEntity(HttpStatus.UNAUTHORIZED);
    }

    /**
     * Fetch patient from FHIR service.
     *
     * @param mrn
     * @return
     */
    @GetMapping(path = "/patient/{mrn}")
    @ResponseBody
    public String getPatient(@PathVariable("mrn") String mrn) {
        return fhirService.serialize(fhirService.getPatient(mrn));
    }

    /**
     * Create/update entry in EMERSE patient table.
     *
     * @param patient
     * @return
     */
    @PostMapping(path = "/patient")
    public ResponseEntity updatePatient(@RequestBody Patient patient) {
        databaseService.updatePatient(patient);
        return new ResponseEntity(HttpStatus.OK);
    }

    /**
     * Fetch a patient's documents.
     *
     * @param patientId
     * @return
     */
    @GetMapping(path = "/documents/{patientId}")
    @ResponseBody
    public List<?> getDocuments(@PathVariable("patientId") String patientId) {
        List<Map<String, Object>> docs = new ArrayList<>();
        Bundle bundle = fhirService.getDocumentBundle(patientId);

        for (Bundle.BundleEntryComponent entry: bundle.getEntry()) {
            DocumentReference documentReference = (DocumentReference) entry.getResource();
            DocumentContent documentContent = fhirService.getDocumentContent(documentReference);

            if (documentContent != null) {
                Map<String, Object> map = new HashMap<>();
                map.put("title", documentReference.getType().getText());
                map.put("date", documentReference.getCreated());
                map.put("body", documentContent.getContent());
                map.put("isHtml", "text/html".equals(documentContent.getContentType()));
                docs.add(map);
            }
        }

        return docs;
    }

    /**
     * Index one or more documents.
     *
     * @param payload
     * @return
     */
    @PostMapping(path = "/index")
    public ResponseEntity index(@RequestBody String payload) {
        return new ResponseEntity(HttpStatus.OK);
    }

}

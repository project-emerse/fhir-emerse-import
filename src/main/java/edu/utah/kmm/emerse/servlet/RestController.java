package edu.utah.kmm.emerse.servlet;

import ca.uhn.fhir.model.dstu2.resource.Patient;
import edu.utah.kmm.emerse.database.DatabaseService;
import edu.utah.kmm.emerse.fhir.FhirService;
import edu.utah.kmm.emerse.solr.SolrService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

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

    /**
     * Authenticate user.
     *
     * @param credentials The user's credentials.
     * @return OK if success; UNAUTHORIZED otherwise.
     */
    @RequestMapping(path = "/authenticate", method = RequestMethod.POST)
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
    @RequestMapping(path = "/patient/{mrn}", method = RequestMethod.GET)
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
    @RequestMapping(path = "/patient", method = RequestMethod.POST)
    public ResponseEntity updatePatient(@RequestBody Patient patient) {
        databaseService.updatePatient(patient);
        return new ResponseEntity(HttpStatus.OK);
    }

    /**
     * Fetch a patient's documents.
     *
     * @param payload
     * @return
     */
    @RequestMapping(path = "/documents", method = RequestMethod.GET)
    public ResponseEntity getDocuments(@RequestBody String payload) {
        return new ResponseEntity(HttpStatus.OK);
    }

    /**
     * Index one or more documents.
     *
     * @param payload
     * @return
     */
    @RequestMapping(path = "/index", method = RequestMethod.POST)
    public ResponseEntity index(@RequestBody String payload) {
        return new ResponseEntity(HttpStatus.OK);
    }

}

package edu.utah.kmm.emerse.epic;

import edu.utah.kmm.emerse.fhir.FhirClient;
import edu.utah.kmm.emerse.fhir.IPatientLookup;
import edu.utah.kmm.emerse.security.Credentials;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.Patient;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Epic requires using web services (which require basic authentication) to retrieve a patient
 * when using OAuth2 (because the access token has not yet been retrieved at this point).
 */
public class EpicPatientLookup implements IPatientLookup {

    private static final String GET_IDENTIFIERS = "epic/2015/Common/Patient/GetPatientIdentifiers/Patient/Identifiers";

    private String userid;

    private FhirClient fhirClient;

    @Autowired
    private EpicService epicService;

    @Override
    public String getName() {
        return "EPIC";
    }

    @Override
    public void initialize(FhirClient fhirClient) {
        this.fhirClient = fhirClient;
        userid = StringUtils.substringAfter(fhirClient.getCredentials().getUsername(), "emp$");
    }

    @Override
    public Patient lookupByMrn(String id) {
        try {
            Map<String, String> body = new HashMap<>();
            body.put("PatientID", id);
            body.put("PatientIDType", "EPICMRN");
            body.put("UserID", userid);
            body.put("UserIDType", "EXTERNAL");
            Map<String, Object> result = epicService.post(GET_IDENTIFIERS, body, true, Map.class, true);
            List<Map<String, String>> identifiers = (List<Map<String, String>>) result.get("Identifiers");

            for (Map<String, String> entry : identifiers) {
                String type = entry.get("IDType");

                if ("FHIR STU3".equals(type)) {
                    String patid = entry.get("ID");
                    return fhirClient.getPatientById(patid);
                }
            }
        } catch (Exception e) {
            // NOP
        }

        return null;
   }

}

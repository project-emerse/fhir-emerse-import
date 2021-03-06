package edu.utah.kmm.emerse.epic;

import edu.utah.kmm.emerse.patient.IPatientLookup;
import edu.utah.kmm.emerse.patient.PatientService;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.Patient;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
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

    @Autowired
    private PatientService patientService;

    @Autowired
    private EpicService epicService;

    @Override
    public String getName() {
        return "EPIC";
    }

    @PostConstruct
    private void init() {
        userid = StringUtils.substringAfter(epicService.getCredentials().getUsername(), "emp$");
    }

    @Override
    public Patient lookupByMRN(String mrn) {
        Map<String, String> body = new HashMap<>();
        body.put("PatientID", mrn);
        body.put("PatientIDType", "EPICMRN");
        body.put("UserID", userid);
        body.put("UserIDType", "EXTERNAL");
        Map<String, Object> result = epicService.post(GET_IDENTIFIERS, body, true, Map.class, true);
        List<Map<String, String>> identifiers = (List<Map<String, String>>) result.get("Identifiers");

        for (Map<String, String> entry : identifiers) {
            String type = entry.get("IDType");

            if ("FHIR STU3".equals(type)) {
                String patid = entry.get("ID");
                Patient patient = patientService.getPatientById(patid);

                if (patientService.extractMRN(patient) == null) {
                    patient.addIdentifier(patientService.createMRN(mrn));
                }

                return patient;
            }
        }

        return null;
    }

}

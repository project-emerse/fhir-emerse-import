package edu.utah.kmm.emerse.epic;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import edu.utah.kmm.emerse.fhir.IPatientLookup;
import edu.utah.kmm.emerse.security.Credentials;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.dstu3.model.HumanName;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Patient;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Epic requires using web services (which require basic authentication) to retrieve a patient
 * when using OAuth2 (because the access token has not yet been retrieved at this point).
 */
public class EpicPatientLookup implements IPatientLookup {

    private static final String GET_DEMOGRAPHICS = "epic/2017/Common/Patient/GetPatientDemographics/Patient/Demographics";

    private static final String GET_IDENTIFIERS = "epic/2015/Common/Patient/GetPatientIdentifiers/Patient/Identifiers";

    private static final SimpleDateFormat dateParser = new SimpleDateFormat("yyyy-MM-dd");

    // TODO: where are the OID mappings?
    private static final String[] CODE_MAPPINGS = {
            "EPI=0",
            "EPICMRN=1",
            "CEID=",
            "EXTERNAL=",
            "FHIR=",
            "FHIR STU3=",
            "INTERNAL=",
            "MYCHARTLOGIN=",
            "WPRINTERNAL="
    };

    private static Date parseDate(String dateStr) {
        try {
            return dateStr ==  null ? null : dateParser.parse(dateStr);
        } catch (Exception e) {
            return null;
        }
    }

    private static AdministrativeGender parseGender(String genderString) {
        try {
            return genderString == null ? null : AdministrativeGender.valueOf(genderString.toUpperCase());
        } catch (Exception e) {
            return null;
        }
    }

    private static CodeableConcept parseMaritalStatus(String statusStr) {
       return statusStr == null ? null : new CodeableConcept().setText(statusStr);
    }

    private static HumanName parseName(Map<String, String> nameMap) {
        if (nameMap == null || nameMap.isEmpty()) {
            return null;
        }

        HumanName name = new HumanName();
        name.setFamily(StringUtils.trimToNull(nameMap.get("LastName")));
        name.addGiven(StringUtils.trimToNull(nameMap.get("FirstName")));
        name.addGiven(StringUtils.trimToNull(nameMap.get("MiddleName")));
        name.addPrefix(StringUtils.trimToNull(nameMap.get("Title")));
        name.addSuffix(StringUtils.trimToNull(nameMap.get("Suffix")));
        return name;
    }

    private final Map<String, String> oidToEpic = new HashMap<>();

    private final Map<String, String> epicToOid = new HashMap<>();

    private Credentials credentials;

    private EpicService epicService;

    @Override
    public String getName() {
        return "EPIC";
    }

    @Override
    public void initialize(IGenericClient client, Credentials credentials) {
        this.credentials = credentials;
        epicService = new EpicService(client, credentials);
        initMappings();
    }

    private void initMappings() {
        for (String mapping: CODE_MAPPINGS) {
            String[] pcs = mapping.split("\\=", 2);

            if (!pcs[1].isEmpty()) {
                String epic = pcs[0];
                String oid = "urn:oid:1.2.840.114350.1.13.90.3.7.5.737384." + pcs[1];
                oidToEpic.put(oid, epic);
                epicToOid.put(epic, oid);
            }
        }
    }

    @Override
    public Patient lookupByIdentifier(String system, String id) {
        String epicId = oidToEpic.get(system);

        if (epicId == null) {
            return null;
        }

        Map<String, String> body = new HashMap<>();
        body.put("PatientID", id);
        body.put("PatientIDType", epicId);
        body.put("UserID", StringUtils.substringAfter(credentials.getUsername(), "emp$"));
        body.put("UserIDType", "EXTERNAL");
        Map<String, Object> result = epicService.post(GET_IDENTIFIERS, body, true, Map.class);
        List<Map<String, String>> identifiers = (List<Map<String, String>>) result.get("Identifiers");
        Patient patient = new Patient();

        for (Map<String, String> entry: identifiers) {
            String code = entry.get("ID");
            String type = entry.get("IDType");
            String oid = epicToOid.get(type);

            if (oid != null) {
                Identifier identifier = new Identifier();
                identifier.setSystem(oid);
                identifier.setValue(code);
                patient.getIdentifier().add(identifier);
            } else if ("FHIR STU3".equals(type)) {
                patient.setId(code);
            }
        }

        result = epicService.post(GET_DEMOGRAPHICS, body, true, Map.class);
        String birthDate = (String) result.get("DateOfBirth");
        String maritalStatus = (String) result.get("MaritalStatus");
        String gender = (String) result.get("SexAssignedAtBirth");
        Map<String, String> name = (Map<String, String>) result.get("Name");

        patient.setBirthDate(parseDate(birthDate));
        patient.setGender(parseGender(gender));
        patient.setMaritalStatus(parseMaritalStatus(maritalStatus));
        patient.addName(parseName(name));
        return patient;
    }
}

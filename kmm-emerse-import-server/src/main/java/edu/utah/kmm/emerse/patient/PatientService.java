package edu.utah.kmm.emerse.patient;

import edu.utah.kmm.emerse.fhir.FhirService;
import edu.utah.kmm.emerse.fhir.IdentifierType;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Patient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.Assert;

public class PatientService {

    @Autowired
    private FhirService fhirService;

    @Autowired
    private PatientLookupRegistry patientLookupRegistry;

    @Value("${fhir.server.patientlookup:DEFAULT}")
    private String patientLookupType;

    @Value("${fhir.mrn.system}")
    private String mrnSystem;

    private IPatientLookup patientLookup;

    private void init() {
        patientLookup = patientLookupRegistry.get(patientLookupType);
        Assert.notNull(patientLookup, "Unknown patient lookup plugin: " + patientLookupType);
        patientLookup.initialize(fhirService);
    }

    public Patient getPatientById(String patid) {
        return fhirService.readResource(Patient.class, patid);
    }

    public Patient getPatient(String id, IdentifierType type) {
        Assert.isTrue(type == IdentifierType.MRN || type == IdentifierType.PATID, "Invalid identifier type.");
        return type == IdentifierType.MRN ? getPatientByMrn(id) : getPatientById(id);
    }

    public Patient getPatientByMrn(String mrn) {
        return patientLookup.lookupByMRN(mrn);
    }

    public String extractMRN(Patient patient) {
        for (Identifier identifier: patient.getIdentifier()) {
            if (mrnSystem.equals(identifier.getSystem())) {
                return identifier.getValue();
            }
        }

        return null;
    }


}

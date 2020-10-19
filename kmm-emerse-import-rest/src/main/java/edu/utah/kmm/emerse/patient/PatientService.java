package edu.utah.kmm.emerse.patient;

import edu.utah.kmm.emerse.fhir.FhirService;
import edu.utah.kmm.emerse.fhir.IdentifierType;
import edu.utah.kmm.emerse.util.MiscUtil;
import org.apache.zookeeper.data.Id;
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

    private volatile IPatientLookup patientLookup;

    private IPatientLookup getPatientLookup() {
        if (patientLookup == null) {
            synchronized (patientLookupRegistry) {
                if (patientLookup == null) {
                    patientLookup = patientLookupRegistry.get(patientLookupType);
                    Assert.notNull(patientLookup, () -> "Unknown patient lookup plugin: " + patientLookupType);
                }
            }
        }

        return patientLookup;
    }

    public Patient getPatientById(String patid) {
        return fhirService.readResource(Patient.class, patid);
    }

    public Patient getPatient(
            String id,
            IdentifierType type) {
        MiscUtil.validateIdentiferType(type, IdentifierType.MRN, IdentifierType.PATID);
        return type == IdentifierType.MRN ? getPatientByMrn(id) : getPatientById(id);
    }

    public Patient getPatientByMrn(String mrn) {
        return getPatientLookup().lookupByMRN(mrn);
    }

    public String extractMRN(Patient patient) {
        for (Identifier identifier : patient.getIdentifier()) {
            if (mrnSystem.equals(identifier.getSystem())) {
                return identifier.getValue();
            }
        }

        return null;
    }

    public Identifier createMRN(String mrn) {
        return new Identifier().setSystem(mrnSystem).setValue(mrn);
    }

}

package edu.utah.kmm.emerse.patient;

import edu.utah.kmm.emerse.fhir.FhirService;
import edu.utah.kmm.emerse.fhir.IdentifierType;
import edu.utah.kmm.emerse.util.MiscUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Patient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.Assert;

/**
 * Service for patient-related operations.
 */
public class PatientService {

    private static final Log log = LogFactory.getLog(PatientService.class);

    @Autowired
    private FhirService fhirService;

    @Autowired
    private PatientLookupRegistry patientLookupRegistry;

    @Value("${fhir.server.patientlookup:DEFAULT}")
    private String patientLookupType;

    @Value("${fhir.mrn.system}")
    private String mrnSystem;

    private volatile IPatientLookup patientLookup;

    /**
     * Returns the service to use for patient lookup.
     */
    private IPatientLookup getPatientLookup() {
        if (patientLookup == null) {
            synchronized (patientLookupRegistry) {
                if (patientLookup == null) {
                    patientLookup = patientLookupRegistry.get(patientLookupType);
                    Assert.notNull(patientLookup, () -> "Unknown patient lookup plugin: " + patientLookupType);
                    log.info("Using patient lookup plugin: " + patientLookupType);
                }
            }
        }

        return patientLookup;
    }

    /**
     * Returns a patient resource given its FHIR id.
     *
     * @param patid The FHIR id.
     * @return The associated patient resource.
     */
    public Patient getPatientById(String patid) {
        return fhirService.readResource(Patient.class, patid);
    }

    /**
     * Returns a patient resource given an identifier.
     *
     * @param id The identifier.
     * @param type The type of identifier.
     * @return The associated patient resource.
     */
    public Patient getPatient(
            String id,
            IdentifierType type) {
        MiscUtil.validateIdentiferType(type, IdentifierType.MRN, IdentifierType.PATID);
        return type == IdentifierType.MRN ? getPatientByMrn(id) : getPatientById(id);
    }

    /**
     * Returns a patient resource given its MRN.
     *
     * @param mrn The patient's MRN.
     * @return The associated patient resource.
     */
    public Patient getPatientByMrn(String mrn) {
        return getPatientLookup().lookupByMRN(mrn);
    }

    /**
     * Returns the MRN for the given patient using the MRN system configured for this service.
     *
     * @param patient The patient resource.
     * @return The MRN, or null if none found.
     */
    public String extractMRN(Patient patient) {
        return patient.getIdentifier().stream()
                .filter(identifier -> mrnSystem.equals(identifier.getSystem()))
                .map(Identifier::getValue)
                .findFirst()
                .orElse(null);
    }

    /**
     * Convenience method for creating an MRN identifier.
     *
     * @param mrn The MRN.
     * @return An identifier for the MRN.
     */
    public Identifier createMRN(String mrn) {
        return new Identifier().setSystem(mrnSystem).setValue(mrn);
    }

}

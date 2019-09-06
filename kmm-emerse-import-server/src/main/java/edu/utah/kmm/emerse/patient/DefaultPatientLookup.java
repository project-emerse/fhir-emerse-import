package edu.utah.kmm.emerse.patient;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import edu.utah.kmm.emerse.fhir.FhirService;
import edu.utah.kmm.emerse.patient.IPatientLookup;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Patient;
import org.springframework.beans.factory.annotation.Value;

public class DefaultPatientLookup implements IPatientLookup {

    @Value("${fhir.mrn.system}")
    private String mrnSystem;

    private IGenericClient client;

    @Override
    public String getName() {
        return "DEFAULT";
    }

    @Override
    public void initialize(FhirService fhirService) {
        this.client = fhirService.getGenericClient();
    }

    @Override
    public Patient lookupByMRN(String mrn) {
        Bundle bundle = client.search()
                .forResource(Patient.class)
                .where(Patient.IDENTIFIER.exactly().systemAndCode(mrnSystem, mrn))
                .returnBundle(Bundle.class)
                .execute();

        return (Patient) bundle.getEntryFirstRep().getResource();
    }

}

package edu.utah.kmm.emerse.fhir;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import edu.utah.kmm.emerse.security.Credentials;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Patient;

public class DefaultPatientLookup implements IPatientLookup {

    private IGenericClient client;

    @Override
    public String getName() {
        return "DEFAULT";
    }

    @Override
    public void initialize(IGenericClient client, Credentials credentials) {
        this.client = client;
    }

    @Override
    public Patient lookupByIdentifier(String system, String id) {
        Bundle bundle = client.search()
                .forResource(Patient.class)
                .where(Patient.IDENTIFIER.exactly().systemAndCode(system, id))
                .returnBundle(Bundle.class)
                .execute();

        return (Patient) bundle.getEntryFirstRep().getResource();
    }
}

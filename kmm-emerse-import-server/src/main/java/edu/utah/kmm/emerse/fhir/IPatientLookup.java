package edu.utah.kmm.emerse.fhir;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import edu.utah.kmm.emerse.security.Credentials;
import org.hl7.fhir.dstu3.model.Patient;

public interface IPatientLookup {

    String getName();

    void initialize(IGenericClient client, Credentials credentials);

    Patient lookupByIdentifier(String system, String id);

}

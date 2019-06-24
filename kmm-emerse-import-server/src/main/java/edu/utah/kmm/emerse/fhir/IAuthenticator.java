package edu.utah.kmm.emerse.fhir;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import edu.utah.kmm.emerse.security.Credentials;

public interface IAuthenticator {

    String getName();

    void initialize(IGenericClient client, Credentials credentials);

    void authenticate(String patientId);
}

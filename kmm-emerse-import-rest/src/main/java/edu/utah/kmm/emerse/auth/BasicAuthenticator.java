package edu.utah.kmm.emerse.auth;

import ca.uhn.fhir.rest.client.api.IClientInterceptor;
import ca.uhn.fhir.rest.client.interceptor.BasicAuthInterceptor;
import edu.utah.kmm.emerse.fhir.FhirService;
import edu.utah.kmm.emerse.security.Credentials;

/**
 * Authenticator for basic authentication.
 */
public class BasicAuthenticator implements IAuthenticator {
    @Override
    public String getName() {
        return "BASIC";
    }

    @Override
    public void initialize(FhirService fhirService) {
        Credentials credentials = fhirService.getCredentials();
        IClientInterceptor interceptor = new BasicAuthInterceptor(
                credentials.getUsername(), credentials.getPassword());
        fhirService.getGenericClient().registerInterceptor(interceptor);
    }

}

package edu.utah.kmm.emerse.fhir;

import ca.uhn.fhir.rest.client.api.IClientInterceptor;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.BasicAuthInterceptor;
import edu.utah.kmm.emerse.security.Credentials;

public class BasicAuthenticator implements IAuthenticator {
    @Override
    public String getName() {
        return "BASIC";
    }

    @Override
    public void initialize(IGenericClient client, Credentials credentials) {
        IClientInterceptor interceptor = new BasicAuthInterceptor(
                credentials.getUsername(), credentials.getPassword());
        client.registerInterceptor(interceptor);
    }

}

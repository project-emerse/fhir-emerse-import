package edu.utah.kmm.emerse.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.EncodingEnum;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.AdditionalRequestHeadersInterceptor;
import edu.utah.kmm.emerse.security.Credentials;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.Assert;

/**
 * FHIR-related services.
 */
public class FhirClientFactory {

    @Autowired
    private FhirContext fhirContext;
    
    @Autowired
    private Credentials fhirServiceCredentials;

    @Autowired
    private AuthenticatorRegistry authenticatorRegistry;

    @Autowired
    private PatientLookupRegistry patientLookupRegistry;

    @Value("${fhir.server.root}")
    private String fhirRoot;

    @Value("${fhir.mrn.system}")
    private String mrnSystem;

    @Value("${fhir.server.authtype}")
    private String authenticationType;

    @Value("${fhir.server.patientlookup:DEFAULT}")
    private String patientLookupType;

    @Value("${fhir.server.headers:}")
    private String extraHeaders;

    private IAuthenticator authenticator;

    private IPatientLookup patientLookup;

    private void init() {
        authenticator = authenticatorRegistry.get(authenticationType);
        Assert.notNull(authenticator, "Unrecognized authentication scheme: " + authenticationType);
        patientLookup = patientLookupRegistry.get(patientLookupType);
        Assert.notNull(patientLookup, "Unknown patient lookup plugin: " + patientLookupType);
        patientLookup.initialize(newGenericClient(), fhirServiceCredentials);
    }
    
    public IGenericClient newGenericClient() {
        IGenericClient fhirClient = fhirContext.newRestfulGenericClient(fhirRoot);
        fhirClient.setEncoding(EncodingEnum.JSON);

        if (!extraHeaders.isEmpty()) {
            AdditionalRequestHeadersInterceptor interceptor = new AdditionalRequestHeadersInterceptor();

            for(String header: extraHeaders.split("\\n")) {
                String[] pcs = header.split("\\:", 2);
                interceptor.addHeaderValue(pcs[0].trim(), pcs.length == 2 ? pcs[1].trim() : "");
            }

            fhirClient.registerInterceptor(interceptor);
        }

        authenticator.initialize(fhirClient, fhirServiceCredentials);
        return fhirClient;
    }

    public FhirClient newFhirClient() {
        return new FhirClient(newGenericClient(), authenticator, patientLookup);
    }
}

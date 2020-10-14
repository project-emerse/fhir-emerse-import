package edu.utah.kmm.emerse.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.EncodingEnum;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.AdditionalRequestHeadersInterceptor;
import edu.utah.kmm.emerse.auth.AuthenticatorRegistry;
import edu.utah.kmm.emerse.auth.IAuthenticator;
import edu.utah.kmm.emerse.security.Credentials;
import org.hl7.fhir.dstu3.model.CapabilityStatement;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.Assert;

/**
 * FHIR-related services.
 */
public class FhirService {

    @Value("${fhir.server.root}")
    private String fhirRoot;

    @Value("${fhir.server.authtype}")
    private String authenticationType;

    @Value("${fhir.server.headers:}")
    private String extraHeaders;

    @Autowired
    private FhirContext fhirContext;

    @Autowired
    private Credentials fhirServiceCredentials;

    @Autowired
    private AuthenticatorRegistry authenticatorRegistry;

    private IGenericClient genericClient;

    private IAuthenticator authenticator;

    private CapabilityStatement capabilityStatement;

    public FhirService() {
    }

    private void init() {
        initGenericClient();
        authenticator = authenticatorRegistry.get(authenticationType);
        Assert.notNull(authenticator, () -> "Unrecognized authentication scheme: " + authenticationType);
        authenticator.initialize(this);
    }

    private void initGenericClient() {
        genericClient = fhirContext.newRestfulGenericClient(fhirRoot);
        genericClient.setEncoding(EncodingEnum.JSON);

        if (!extraHeaders.isEmpty()) {
            AdditionalRequestHeadersInterceptor interceptor = new AdditionalRequestHeadersInterceptor();

            for(String header: extraHeaders.split("\\n")) {
                String[] pcs = header.split("\\:", 2);
                interceptor.addHeaderValue(pcs[0].trim(), pcs.length == 2 ? pcs[1].trim() : "");
            }

            genericClient.registerInterceptor(interceptor);
        }

        capabilityStatement = genericClient.capabilities().ofType(CapabilityStatement.class).execute();
    }

    public IGenericClient getGenericClient() {
        return genericClient;
    }

    public Credentials getCredentials() {
        return fhirServiceCredentials;
    }

    public CapabilityStatement getCapabilityStatement() {
        return capabilityStatement;
    }

    public <T extends IBaseResource> T readResource(Class<T> type, String fhirId) {
        try {
            return genericClient.read()
                    .resource(type)
                    .withId(fhirId)
                    .execute();
        } catch (Exception e) {
            return null;
        }
    }

    public String serialize(IBaseResource resource) {
        return resource == null ? null : genericClient.getFhirContext().newJsonParser().encodeResourceToString(resource);
    }

    public <T extends IBaseResource> T deserialize(String data, Class<T> resourceType) {
        return data == null ? null : (T) genericClient.getFhirContext().newJsonParser().parseResource(data);
    }

}

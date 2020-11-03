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

    private volatile IGenericClient genericClient;

    private CapabilityStatement capabilityStatement;

    public FhirService() {
    }

    /**
     * Returns the generic client, initializing it if necessary.
     *
     * @return The generic client.
     */
    private synchronized IGenericClient initGenericClient() {
        if (genericClient == null) {
            genericClient = fhirContext.newRestfulGenericClient(fhirRoot);
            genericClient.setEncoding(EncodingEnum.JSON);

            if (!extraHeaders.isEmpty()) {
                AdditionalRequestHeadersInterceptor interceptor = new AdditionalRequestHeadersInterceptor();

                for (String header : extraHeaders.split("\\n")) {
                    String[] pcs = header.split(":", 2);
                    interceptor.addHeaderValue(pcs[0].trim(), pcs.length == 2 ? pcs[1].trim() : "");
                }

                genericClient.registerInterceptor(interceptor);
            }

            capabilityStatement = genericClient.capabilities().ofType(CapabilityStatement.class).execute();
            IAuthenticator authenticator = authenticatorRegistry.get(authenticationType);
            Assert.notNull(authenticator, () -> "Unrecognized authentication scheme: " + authenticationType);
            authenticator.initialize(this);
        }

        return genericClient;
    }

    /**
     * Returns the generic client.
     */
    public IGenericClient getGenericClient() {
        return genericClient == null ? initGenericClient() : genericClient;
    }

    /**
     * Returns the FHIR server credentials.
     */
    public Credentials getCredentials() {
        return fhirServiceCredentials;
    }

    /**
     * Returns the FHIR server's metadata.
     */
    public CapabilityStatement getCapabilityStatement() {
        getGenericClient();
        return capabilityStatement;
    }

    /**
     * Reads a FHIR resource of the specified type using the specified FHIR id.
     *
     * @param type The type of resource to retrieve.
     * @param fhirId The FHIR id of the resource.
     * @param <T> The resource type.
     * @return The resource that was fetched.
     */
    public <T extends IBaseResource> T readResource(Class<T> type, String fhirId) {
        return getGenericClient().read()
                .resource(type)
                .withId(fhirId)
                .execute();
    }

    /**
     * Serialize a resource.
     *
     * @param resource The resource to serialize.
     * @return The serialized form of the resource.
     */
    public String serialize(IBaseResource resource) {
        return resource == null ? null : getGenericClient().getFhirContext().newJsonParser().encodeResourceToString(resource);
    }

    /**
     * Deserialize a resource.
     *
     * @param data The serialized resource.
     * @param resourceType The type of the resource.
     * @param <T> The type of the resource.
     * @return The deserialized resource.
     */
    public <T extends IBaseResource> T deserialize(String data, Class<T> resourceType) {
        return data == null ? null : (T) getGenericClient().getFhirContext().newJsonParser().parseResource(data);
    }

}

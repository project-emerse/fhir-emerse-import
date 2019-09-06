package edu.utah.kmm.emerse.oauth;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import edu.utah.kmm.emerse.fhir.FhirClient;
import edu.utah.kmm.emerse.fhir.IAuthenticator;
import edu.utah.kmm.emerse.security.Credentials;
import org.hl7.fhir.dstu3.model.CapabilityStatement;
import org.hl7.fhir.dstu3.model.Extension;
import org.hl7.fhir.dstu3.model.UriType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;

public abstract class BaseOAuth2Authenticator implements IAuthenticator {

    private static final String OAUTH_EXTENSION = "http://fhir-registry.smarthealthit.org/StructureDefinition/oauth-uris";

    protected String authorizeEndpoint;

    protected String tokenEndpoint;

    private String fhirEndpoint;

    @Override
    public void initialize(FhirClient fhirClient) {
        IGenericClient client = fhirClient.getGenericClient();
        fhirEndpoint = client.getServerBase();
        fhirEndpoint = fhirEndpoint.endsWith("/") ? fhirEndpoint : fhirEndpoint + "/";
        getOAuthEndpoints(client);
    }

    private void getOAuthEndpoints(IGenericClient client) {
        CapabilityStatement cp = client.capabilities().ofType(CapabilityStatement.class).execute();

        for (Extension ext: cp.getRest().get(0).getSecurity().getExtension()) {
            if (OAUTH_EXTENSION.equals(ext.getUrl())) {
                for (Extension ext2: ext.getExtension()) {
                    String url = ext2.getUrl();

                    if ("authorize".equals(url)) {
                        authorizeEndpoint = getUri(ext2);
                    } else if ("token".equals(url)) {
                        tokenEndpoint = getUri(ext2);
                    }
                }

                break;
            }
        }

        Assert.notNull(authorizeEndpoint, "Could not discover authorization endpoint");
        Assert.notNull(tokenEndpoint, "Could not discover token endpoint");
    }

    private String getUri(Extension extension) {
        return normalizeUri(((UriType) extension.getValue()).getValue());
    }

    private String normalizeUri(String uri) {
        return uri.startsWith("http") ? uri : fhirEndpoint + uri;
    }

    protected String encode(String value) {
        try {
            return URLEncoder.encode(value, "ASCII");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

}

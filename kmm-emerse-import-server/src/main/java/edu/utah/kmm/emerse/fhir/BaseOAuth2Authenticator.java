package edu.utah.kmm.emerse.fhir;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import edu.utah.kmm.emerse.security.Credentials;
import org.hl7.fhir.dstu3.model.CapabilityStatement;
import org.hl7.fhir.dstu3.model.Extension;
import org.hl7.fhir.dstu3.model.UriType;
import org.springframework.util.Assert;

public abstract class BaseOAuth2Authenticator implements IAuthenticator {

    private static final String OAUTH_EXTENSION = "http://fhir-registry.smarthealthit.org/StructureDefinition/oauth-uris";

    protected String authorizeEndpoint;

    protected String tokenEndpoint;

    protected Credentials credentials;

    protected IGenericClient client;

    @Override
    public void initialize(IGenericClient client, Credentials credentials) {
        this.client = client;
        this.credentials = credentials;
        getOAuthEndpoints();
    }

    private void getOAuthEndpoints() {
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
        return ((UriType) extension.getValue()).getValue();
    }


}

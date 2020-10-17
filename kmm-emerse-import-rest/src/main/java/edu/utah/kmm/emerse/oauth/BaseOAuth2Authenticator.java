package edu.utah.kmm.emerse.oauth;

import edu.utah.kmm.emerse.auth.IAuthenticator;
import edu.utah.kmm.emerse.fhir.FhirService;
import org.hl7.fhir.dstu3.model.CapabilityStatement;
import org.hl7.fhir.dstu3.model.Extension;
import org.hl7.fhir.dstu3.model.UriType;
import org.springframework.util.Assert;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public abstract class BaseOAuth2Authenticator implements IAuthenticator {

    private static final String OAUTH_EXTENSION = "http://fhir-registry.smarthealthit.org/StructureDefinition/oauth-uris";

    protected String authorizeEndpoint;

    protected String tokenEndpoint;

    private String fhirEndpoint;

    @Override
    public void initialize(FhirService fhirService) {
        fhirEndpoint = fhirService.getGenericClient().getServerBase();
        fhirEndpoint = fhirEndpoint.endsWith("/") ? fhirEndpoint : fhirEndpoint + "/";
        getOAuthEndpoints(fhirService.getCapabilityStatement());
    }

    private void getOAuthEndpoints(CapabilityStatement cp) {
        for (Extension ext : cp.getRest().get(0).getSecurity().getExtension()) {
            if (OAUTH_EXTENSION.equals(ext.getUrl())) {
                for (Extension ext2 : ext.getExtension()) {
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

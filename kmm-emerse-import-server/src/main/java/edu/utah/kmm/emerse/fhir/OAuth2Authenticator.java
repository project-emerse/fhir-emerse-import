package edu.utah.kmm.emerse.fhir;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import edu.utah.kmm.emerse.security.Credentials;
import edu.utah.kmm.emerse.util.MiscUtil;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.client.utils.URIUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

public class OAuth2Authenticator extends BaseOAuth2Authenticator {

    private static final String SCOPES = "patient/*.read";

    private TokenResponse tokenResponse;

    @Override
    public String getName() {
        return "OAUTH2";
    }

    @Override
    public void initialize(IGenericClient client, Credentials credentials) {
        super.initialize(client, credentials);
        authenticate(null);
    }

    @Override
    public void authenticate(String patientId) {
        if (tokenResponse == null || tokenResponse.isExpired()) {
            tokenResponse = null;
            RestTemplate restTemplate = new RestTemplate();
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.set("grant_type", "client_credentials");
            params.set("scope", SCOPES);
            HttpHeaders headers = new HttpHeaders();
            URI ep = MiscUtil.toURI(tokenEndpoint);
            headers.set("Authorization", "Basic "
                    + Base64.encodeBase64String((credentials.getUsername() + ":" + credentials.getPassword()).getBytes()));
            RequestEntity<Object> request = new RequestEntity<Object>(params, headers, HttpMethod.POST, ep);
            ResponseEntity<TokenResponse> response = restTemplate.exchange(request, TokenResponse.class);
            tokenResponse = response.getBody();
        }

    }

}

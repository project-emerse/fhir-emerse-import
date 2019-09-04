package edu.utah.kmm.emerse.oauth;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import edu.utah.kmm.emerse.security.Credentials;
import edu.utah.kmm.emerse.util.MiscUtil;
import org.apache.commons.codec.binary.Base64;
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

    private class OAuth2Interceptor extends OAuthInterceptor {

        @Override
        protected AccessToken generateToken() {
            return generateAccessToken();
        }
    }

    @Override
    public String getName() {
        return "OAUTH2";
    }

    @Override
    public void initialize(IGenericClient client, Credentials credentials) {
        super.initialize(client, credentials);
        client.registerInterceptor(new OAuth2Interceptor());
    }

    private AccessToken generateAccessToken() {
            RestTemplate restTemplate = new RestTemplate();
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.set("grant_type", "client_credentials");
            params.set("scope", encode(SCOPES));
            HttpHeaders headers = new HttpHeaders();
            URI ep = MiscUtil.toURI(tokenEndpoint);
            headers.set("Authorization", "Basic "
                    + Base64.encodeBase64String((credentials.getUsername() + ":" + credentials.getPassword()).getBytes()));
            RequestEntity<Object> request = new RequestEntity<Object>(params, headers, HttpMethod.POST, ep);
            ResponseEntity<AccessToken> response = restTemplate.exchange(request, AccessToken.class);
            return response.getBody();
    }

}

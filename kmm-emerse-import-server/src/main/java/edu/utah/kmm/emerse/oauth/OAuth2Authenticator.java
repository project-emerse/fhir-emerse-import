package edu.utah.kmm.emerse.oauth;

import edu.utah.kmm.emerse.fhir.FhirService;
import edu.utah.kmm.emerse.security.Credentials;
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

    private String authHeader;

    @Override
    public String getName() {
        return "OAUTH2";
    }

    @Override
    public void initialize(FhirService fhirService) {
        super.initialize(fhirService);
        fhirService.getGenericClient().registerInterceptor(new OAuth2Interceptor());
        Credentials credentials = fhirService.getCredentials();
        authHeader = "Basic " + new String(Base64.encodeBase64((credentials.getUsername() + ":" + credentials.getPassword()).getBytes()));
    }

    private AccessToken generateAccessToken() {
        RestTemplate restTemplate = new RestTemplate();
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.set("grant_type", "client_credentials");
        params.set("scope", encode(SCOPES));
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authHeader);
        RequestEntity<Object> request = new RequestEntity<Object>(params, headers, HttpMethod.POST, URI.create(tokenEndpoint));
        ResponseEntity<AccessToken> response = restTemplate.exchange(request, AccessToken.class);
        return response.getBody();
    }

}

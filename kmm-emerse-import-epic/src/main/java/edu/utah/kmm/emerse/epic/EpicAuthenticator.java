package edu.utah.kmm.emerse.epic;

import edu.utah.kmm.emerse.fhir.FhirService;
import edu.utah.kmm.emerse.jwt.JwtService;
import edu.utah.kmm.emerse.oauth.AccessToken;
import edu.utah.kmm.emerse.oauth.BaseOAuth2Authenticator;
import edu.utah.kmm.emerse.oauth.OAuthInterceptor;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * JWT-based authentication as used by Epic.
 */
public class EpicAuthenticator extends BaseOAuth2Authenticator {

    private class EpicAuthInterceptor extends OAuthInterceptor {

        @Override
        protected AccessToken generateToken() {
            return generateAccessToken();
        }

    }

    @Autowired
    private EpicService epicService;

    @Autowired
    private JwtService jwtService;

    @Value("${app.client_id}")
    private String clientId;

    @Override
    public String getName() {
        return "EPIC";
    }

    @Override
    public void initialize(FhirService fhirService) {
        super.initialize(fhirService);
        fhirService.getGenericClient().registerInterceptor(new EpicAuthInterceptor());
    }

    private AccessToken generateAccessToken() {
        return fetchAccessToken(jwtService.newJwt(clientId, tokenEndpoint), tokenEndpoint);
    }

    private AccessToken fetchAccessToken(String jwt, String tokenUrl) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.set("grant_type", "client_credentials");
        params.set("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer");
        params.set("client_assertion", jwt);
        String body = UriComponentsBuilder.newInstance().queryParams(params).build().getQuery();
        return epicService.post(tokenEndpoint, body, false, AccessToken.class, false);
    }

}

package edu.utah.kmm.emerse.epic;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import edu.utah.kmm.emerse.oauth.AccessToken;
import edu.utah.kmm.emerse.oauth.BaseOAuth2Authenticator;
import edu.utah.kmm.emerse.oauth.OAuthInterceptor;
import edu.utah.kmm.emerse.security.Credentials;
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

public class EpicAuthenticator extends BaseOAuth2Authenticator {

    private class EpicAuthInterceptor extends OAuthInterceptor {

        @Override
        protected AccessToken generateToken() {
            return generateAccessToken("e3-ooNeHcmrnIanfYJ.wmEQ3");
        }

    }

    @Autowired
    private EpicService epicService;

    @Value("${app.client_id}")
    private String clientId;

    @Value("${app.redirect_uri}")
    private String redirectUri;

    @Value("${app.scope}")
    private String scope;

    @Value("${epic.userid}")
    private String userId;

    @Override
    public String getName() {
        return "EPIC";
    }

    @Override
    public void initialize(IGenericClient client, Credentials credentials) {
        super.initialize(client, credentials);
        client.registerInterceptor(new EpicAuthInterceptor());
    }

    private AccessToken generateAccessToken(String patientId) {
        String launchToken = fetchLaunchToken(patientId);
        String authorizationCode = fetchAuthorizationCode(launchToken);
        return fetchAccessToken(authorizationCode);
    }

    private String fetchLaunchToken(String patientId) {
        MultiValueMap<String, String> body = newRequestParams(false);
        body.set("patient", patientId);
        body.set("user", userId);
        Map<String, String> result = epicService.post("UU/2017/Security/OAuth2/IssueLaunchToken", body, true, Map.class, true);
        String launchToken = result == null ? null : result.get("launchToken");
        Assert.isTrue(launchToken != null && !launchToken.isEmpty(), "Failed to fetch a launch token.");
        return launchToken;
    }

    private String fetchAuthorizationCode(String launchToken) {
        MultiValueMap<String, String> params = newRequestParams(false);
        params.set("launch", launchToken);
        params.set("state", UUID.randomUUID().toString());
        params.set("scope", encode(scope));
        params.set("response_type", "code");
        ResponseEntity<?> response = epicService.getResponse(authorizeEndpoint, params, null, false);
        URI location = response.getHeaders().getLocation();
        Assert.notNull(location, "Failed to fetch an authorization code.");
        List<NameValuePair> qs = URLEncodedUtils.parse(location, StandardCharsets.UTF_8);
        qs = qs.stream().filter(param -> "code".equals(param.getName())).collect(Collectors.toList());
        Assert.isTrue(!qs.isEmpty(), "Failed to fetch an authorization code.");
        return qs.get(0).getValue();
    }

    private AccessToken fetchAccessToken(String authorizationCode) {
        MultiValueMap<String, String> params = newRequestParams(true);
        params.set("code", authorizationCode);
        params.set("grant_type", "authorization_code");
        String body = UriComponentsBuilder.newInstance().queryParams(params).build().getQuery();
        return epicService.post(tokenEndpoint, body, false, AccessToken.class, false);
    }

    private MultiValueMap<String, String> newRequestParams(boolean encode) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.set("client_id", clientId);
        params.set("redirect_uri", encode ? encode(redirectUri) : redirectUri);
        return params;
    }

}

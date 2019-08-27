package edu.utah.kmm.emerse.epic;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.BearerTokenAuthInterceptor;
import edu.utah.kmm.emerse.fhir.BaseOAuth2Authenticator;
import edu.utah.kmm.emerse.fhir.TokenResponse;
import edu.utah.kmm.emerse.security.Credentials;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class EpicAuthenticator extends BaseOAuth2Authenticator {

    private static final ThreadLocal<BearerTokenAuthInterceptor> oauthInterceptor = new ThreadLocal<BearerTokenAuthInterceptor>() {

        @Override
        protected BearerTokenAuthInterceptor initialValue() {
            return new BearerTokenAuthInterceptor();
        }
    };

    private final Map<String, TokenResponse> accessTokens = new HashMap<>();

    private EpicService epicService;

    @Value("${app.client_id}")
    private String clientId;

    @Value("${app.redirect_uri}")
    private String redirectUri;

    @Override
    public String getName() {
        return "EPIC";
    }

    @Override
    public void initialize(IGenericClient client, Credentials credentials) {
        super.initialize(client, credentials);
        client.registerInterceptor(oauthInterceptor.get());
        epicService = new EpicService(client, credentials);
    }

    @Override
    public void authenticate(String patientId) {
        TokenResponse accessToken = getAccessToken(patientId);
        oauthInterceptor.get().setToken(accessToken.access_token);
    }

    private TokenResponse getAccessToken(String patientId) {
        TokenResponse accessToken = accessTokens.get(patientId);

        if (accessToken == null || accessToken.isExpired()) {
            accessToken = generateAccessToken(patientId);
            accessTokens.put(patientId, accessToken);
        }

        return accessToken;
    }

    private TokenResponse generateAccessToken(String patientId) {
        String launchToken = fetchLaunchToken(patientId);
        String authorizationCode = fetchAuthorizationCode(launchToken);
        return fetchAccessToken(authorizationCode);
    }

    private String fetchLaunchToken(String patientId) {
        MultiValueMap<String, String> body = newRequestParams();
        body.set("patient", patientId);
        body.set("user", "104341");
        Map<String, String> result = epicService.post("UU/2017/Security/OAuth2/IssueLaunchToken", body, true, Map.class);
        String launchToken = result == null ? null : result.get("launchToken");
        Assert.notNull(launchToken, "Failed to fetch a launch token.");
        return launchToken;
    }

    private String fetchAuthorizationCode(String launchToken) {
        MultiValueMap<String, String> params = newRequestParams();
        params.set("launch", launchToken);
        params.set("state", UUID.randomUUID().toString());
        params.set("scope", "launch patient/*.read openid user/*.read profile");
        params.set("response_type", "code");
        ResponseEntity<?> response = epicService.getResponse(authorizeEndpoint, params, null);
        URI location = response.getHeaders().getLocation();
        Assert.notNull(location, "Failed to fetch an authorization code.");
        List<NameValuePair> qs = URLEncodedUtils.parse(location, StandardCharsets.UTF_8);
        qs = qs.stream().filter(param -> "code".equals(param.getName())).collect(Collectors.toList());
        Assert.isTrue(!qs.isEmpty(), "Failed to fetch an authorization code.");
        return qs.get(0).getValue();
    }

    private TokenResponse fetchAccessToken(String authorizationCode) {
        MultiValueMap<String, String> params = newRequestParams();
        params.set("code", authorizationCode);
        params.set("grant_type", "authorization_code");
        String body = UriComponentsBuilder.newInstance().queryParams(params).build().getQuery();
        return epicService.post(tokenEndpoint, body, false, TokenResponse.class);
    }

    private MultiValueMap<String, String> newRequestParams() {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.set("client_id", clientId);
        params.set("redirect_uri", redirectUri);
        return params;
    }

}
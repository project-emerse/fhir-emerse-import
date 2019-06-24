package edu.utah.kmm.emerse.epic;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.BearerTokenAuthInterceptor;
import edu.utah.kmm.emerse.fhir.BaseOAuth2Authenticator;
import edu.utah.kmm.emerse.fhir.TokenResponse;
import edu.utah.kmm.emerse.security.Credentials;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
        params.set("scope", "launch patient/*.* openid user/*.* profile");
        params.set("response_type", "code");
        String result = epicService.get(authorizeEndpoint, params, String.class);
        result = StringUtils.substringBetween(result, "href=\"", "\"");
        params = UriComponentsBuilder.fromHttpUrl(result).build().getQueryParams();
        String code = params == null ? null : params.getFirst("code");
        Assert.notNull(params, "Failed to fetch an authorization code.");
        return code;
    }

    private TokenResponse fetchAccessToken(String authorizationCode) {
        MultiValueMap<String, String> body = newRequestParams();
        body.set("code", authorizationCode);
        body.set("grant_type", "authorization_code");
        return epicService.post(tokenEndpoint, body, false, TokenResponse.class);
    }

    private MultiValueMap<String, String> newRequestParams() {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.set("client_id", clientId);
        params.set("redirect_uri", redirectUri);
        return params;
    }

}

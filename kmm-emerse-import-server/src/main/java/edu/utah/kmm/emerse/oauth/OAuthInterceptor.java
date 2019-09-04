package edu.utah.kmm.emerse.oauth;

import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.client.api.IClientInterceptor;
import ca.uhn.fhir.rest.client.api.IHttpRequest;
import ca.uhn.fhir.rest.client.api.IHttpResponse;

import java.io.IOException;

public abstract class OAuthInterceptor implements IClientInterceptor {

    private volatile AccessToken accessToken;

    protected abstract AccessToken generateToken();

    private synchronized String getToken() {
        if (accessToken == null || accessToken.isExpired()) {
            accessToken = generateToken();
        }

        return accessToken.access_token;
    }

    @Override
    public void interceptRequest(IHttpRequest theRequest) {
        theRequest.addHeader(Constants.HEADER_AUTHORIZATION, (Constants.HEADER_AUTHORIZATION_VALPREFIX_BEARER + getToken()));
    }

    @Override
    public void interceptResponse(IHttpResponse theResponse) throws IOException {
        // NOP
    }
}

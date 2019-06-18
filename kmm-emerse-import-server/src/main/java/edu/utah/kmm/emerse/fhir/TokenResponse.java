package edu.utah.kmm.emerse.fhir;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class TokenResponse {

    String access_token;

    String token_type;

    long expires_in;

    String scope;

    long expirationTime;

    @JsonCreator
    public TokenResponse(
            @JsonProperty("access_token") String access_token,
            @JsonProperty("token_type") String token_type,
            @JsonProperty("expires_in") long expires_in
    ) {
        this(access_token, token_type, expires_in, null);
    }

    @JsonCreator
    public TokenResponse(
            @JsonProperty("access_token") String access_token,
            @JsonProperty("token_type") String token_type,
            @JsonProperty("expires_in") long expires_in,
            @JsonProperty("scope") String scope
    ) {
        this.access_token = access_token;
        this.token_type = token_type;
        this.expires_in = expires_in;
        this.scope = scope;
        expirationTime = System.currentTimeMillis() + expires_in * 1000;
    }

    boolean isExpired() {
        return System.currentTimeMillis() >= expirationTime;
    }
}

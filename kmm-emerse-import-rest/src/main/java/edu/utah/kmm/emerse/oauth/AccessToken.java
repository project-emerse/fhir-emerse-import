package edu.utah.kmm.emerse.oauth;

/**
 * An OAUTH access token.
 */
public class AccessToken {

    public String access_token;

    public String token_type;

    public long expires_in;

    public String scope;

    private long issued;

    private long expirationTime;

    public AccessToken() {
        issued = System.currentTimeMillis();
    }

    public boolean isExpired() {
        expirationTime = expirationTime == 0 ? issued + expires_in * 1000 : expirationTime;
        return System.currentTimeMillis() >= expirationTime;
    }
}

package edu.utah.kmm.emerse.auth;

import edu.utah.kmm.emerse.fhir.BaseRegistry;

public class AuthenticatorRegistry extends BaseRegistry<IAuthenticator> {

    public AuthenticatorRegistry() {
        super(IAuthenticator.class);
    }

    @Override
    protected String getName(IAuthenticator entry) {
        return entry.getName();
    }
}

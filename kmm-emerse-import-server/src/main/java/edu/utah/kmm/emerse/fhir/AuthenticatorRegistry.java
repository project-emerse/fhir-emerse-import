package edu.utah.kmm.emerse.fhir;

public class AuthenticatorRegistry extends BaseRegistry<IAuthenticator> {

    public AuthenticatorRegistry(IAuthenticator... authenticators) {
        super(IAuthenticator.class, authenticators);
    }

    @Override
    protected String getName(IAuthenticator entry) {
        return entry.getName();
    }
}

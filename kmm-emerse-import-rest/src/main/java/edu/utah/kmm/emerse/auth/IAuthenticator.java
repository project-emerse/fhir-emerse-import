package edu.utah.kmm.emerse.auth;

import edu.utah.kmm.emerse.fhir.FhirService;

/**
 * Implemented by all FHIR service authenticators.
 */
public interface IAuthenticator {

    /**
     * Returns the unique name of the authenticator.
     */
    String getName();

    /**
     * Initializes the FHIR service to use this authenticator.
     *
     * @param fhirService The FHIR service.
     */
    void initialize(FhirService fhirService);

}

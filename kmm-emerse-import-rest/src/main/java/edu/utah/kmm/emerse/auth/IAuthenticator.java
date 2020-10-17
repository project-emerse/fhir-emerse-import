package edu.utah.kmm.emerse.auth;

import edu.utah.kmm.emerse.fhir.FhirService;

public interface IAuthenticator {

    String getName();

    void initialize(FhirService fhirService);

}

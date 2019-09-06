package edu.utah.kmm.emerse.auth;

import edu.utah.kmm.emerse.fhir.IInitializable;

public interface IAuthenticator extends IInitializable {

    String getName();

}

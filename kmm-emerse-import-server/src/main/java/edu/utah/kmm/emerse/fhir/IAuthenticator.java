package edu.utah.kmm.emerse.fhir;

public interface IAuthenticator extends IInitializable {

    String getName();

    void authenticate(String patientId);
}

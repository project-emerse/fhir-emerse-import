package edu.utah.kmm.emerse.fhir;

import org.hl7.fhir.dstu3.model.Patient;

public interface IPatientLookup extends IInitializable {

    String getName();

    Patient lookupByIdentifier(String system, String id);

}
